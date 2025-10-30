package org.example.carpet.service;

import org.example.carpet.client.InventoryClient;
import org.example.carpet.kafka.InventoryEventProducer;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.OrderLineItem;
import org.example.carpet.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests order lifecycle:
 *  - createOrder reserves inventory for each line item
 *  - createOrder sets status=RESERVED
 *  - cancelOrder releases inventory and sets status=CANCELLED
 *
 *  createOrder() 会为每个 SKU 尝试 reserve() 库存
 * 如果库存不够，会自动回滚之前锁的库存
 * createOrder() 生成的订单状态应为 "RESERVED"
 * cancelOrder() 会释放库存并把状态变 CANCELLED
 *
 *  订单状态机：RESERVED → CANCELLED
 * cancel 的时候会释放库存
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    InventoryClient inventoryClient;

    @Mock
    InventoryEventProducer inventoryEventProducer;

    @InjectMocks
    OrderService orderService;

    @Test
    void createOrder_shouldReserveInventoryAndSetStatusReserved() {
        OrderLineItem line1 = OrderLineItem.builder()
                .sku("RUG-RED")
                .name("Red Wool Carpet")
                .quantity(2)
                .price(199.99)
                .build();

        when(inventoryClient.reserve("RUG-RED", 2))
                .thenReturn(true);

        when(orderRepository.save(any(OrderDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        OrderDocument created = orderService.createOrder(
                "buyer@example.com",
                List.of(line1)
        );

        assertEquals("RESERVED", created.getStatus());

        verify(inventoryClient).reserve("RUG-RED", 2);

        verify(inventoryEventProducer).publishInventoryReserved(
                anyString(), // orderId is random UUID
                eq("RUG-RED"),
                eq(2)
        );
    }

    @Test
    void cancelOrder_shouldReleaseInventoryAndMarkCancelled() {
        OrderLineItem lineItem = OrderLineItem.builder()
                .sku("RUG-RED")
                .quantity(2)
                .price(199.99)
                .build();

        OrderDocument reserved = OrderDocument.builder()
                .orderId("ORD-abc")
                .status("RESERVED")
                .items(List.of(lineItem))
                .build();

        when(orderRepository.findByOrderId("ORD-abc"))
                .thenReturn(Optional.of(reserved));

        when(orderRepository.save(any(OrderDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderDocument cancelled = orderService.cancelOrder("ORD-abc");

        assertEquals("CANCELLED", cancelled.getStatus());

        verify(inventoryClient).release("RUG-RED", 2);

        verify(inventoryEventProducer).publishInventoryReleased(
                eq("ORD-abc"),
                eq("RUG-RED"),
                eq(2)
        );
    }
}