package org.example.carpet.service;

import org.example.carpet.kafka.InventoryEventProducer;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.OrderLineItem;
import org.example.carpet.repository.mongo.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.cassandra.core.CassandraTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;                   // Mongo: orders
    @Mock InventoryService inventoryService;                 // 库存服务（原子扣减/回补）
    @Mock InventoryEventProducer inventoryEventProducer;     // Kafka
    @Mock CassandraTemplate cassandraTemplate;               // Cassandra 事件时间线

    @InjectMocks OrderService orderService;

    @Test
    void createOrder_shouldDeductStockAndSetStatusReserved() {
        OrderLineItem line1 = OrderLineItem.builder()
                .sku("RUG-RED")
                .name("Red Wool Carpet")
                .quantity(2)
                .price(199.99)
                .build();

        // 现在使用 inventoryService.reserve(...) 进行库存扣减
        when(inventoryService.reserve("RUG-RED", 2)).thenReturn(true);

        // 保存订单时回传入参
        when(orderRepository.save(any(OrderDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDocument created = orderService.createOrder("buyer@example.com", List.of(line1));

        assertEquals("RESERVED", created.getStatus());
        verify(inventoryService).reserve("RUG-RED", 2);
        verify(orderRepository).save(any(OrderDocument.class));
        // 事件发送（失败不回滚，这里只验证被调用）
        verify(inventoryEventProducer).publishInventoryReserved(anyString(), eq("RUG-RED"), eq(2));
    }

    @Test
    void cancelOrder_shouldRestockAndMarkCancelled() {
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

        when(orderRepository.findByOrderId("ORD-abc")).thenReturn(Optional.of(reserved));
        when(orderRepository.save(any(OrderDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        // 回补本地库存 - 现在通过 inventoryService.release(...)
        when(inventoryService.release("RUG-RED", 2)).thenReturn(true);

        OrderDocument cancelled = orderService.cancelOrder("ORD-abc");

        assertEquals("CANCELLED", cancelled.getStatus());
        verify(inventoryService).release("RUG-RED", 2);
        verify(orderRepository).save(any(OrderDocument.class));
        verify(inventoryEventProducer).publishInventoryReleased(eq("ORD-abc"), eq("RUG-RED"), eq(2));
    }
}
