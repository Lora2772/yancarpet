package org.example.carpet.service;

import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.OrderLineItem;
import org.example.carpet.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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
    InventoryService inventoryService;

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

        when(inventoryService.reserve("RUG-RED", 2))
                .thenReturn(true);

        // when saving, just echo back the object we got
        when(orderRepository.save(any(OrderDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        OrderDocument created = orderService.createOrder(
                "buyer@example.com",
                List.of(line1)
        );

        assertEquals("RESERVED", created.getStatus());
        assertEquals(1, created.getItems().size());
        assertEquals(2, created.getItems().get(0).getQuantity());

        // verify that we tried to reserve stock for each line item
        verify(inventoryService).reserve("RUG-RED", 2);
    }

    @Test
    void cancelOrder_shouldReleaseInventoryAndMarkCancelled() {
        // prepare an existing RESERVED order
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
                .thenAnswer(inv -> inv.getArgument(0));

        OrderDocument cancelled = orderService.cancelOrder("ORD-abc");

        assertEquals("CANCELLED", cancelled.getStatus());
        verify(inventoryService).release("RUG-RED", 2);
    }
}
