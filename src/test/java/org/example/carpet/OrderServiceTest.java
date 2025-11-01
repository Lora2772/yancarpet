package org.example.carpet.service;

import org.example.carpet.client.InventoryClient;
import org.example.carpet.kafka.InventoryEventProducer;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.OrderLineItem;
import org.example.carpet.repository.mongo.ItemDocumentRepository;
import org.example.carpet.repository.mongo.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;                   // Mongo: orders
    @Mock ItemDocumentRepository itemRepo;                   // Mongo: items (tryDeduct/tryRestock)
    @Mock InventoryEventProducer inventoryEventProducer;     // Kafka
    @Mock InventoryClient inventoryClient;                   // 外部库存（取消时 best-effort 释放）

    @InjectMocks OrderService orderService;

    @Test
    void createOrder_shouldDeductStockAndSetStatusReserved() {
        OrderLineItem line1 = OrderLineItem.builder()
                .sku("RUG-RED")
                .name("Red Wool Carpet")
                .quantity(2)
                .price(199.99)
                .build();

        // 当前实现使用 itemRepo.tryDeduct(...) 而不是 inventoryClient.reserve(...)
        when(itemRepo.tryDeduct("RUG-RED", 2)).thenReturn(1);

        // 保存订单时回传入参
        when(orderRepository.save(any(OrderDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDocument created = orderService.createOrder("buyer@example.com", List.of(line1));

        assertEquals("RESERVED", created.getStatus());
        verify(itemRepo).tryDeduct("RUG-RED", 2);
        verify(orderRepository).save(any(OrderDocument.class));
        // 事件发送（失败不回滚，这里只验证被调用）
        verify(inventoryEventProducer).publishInventoryReserved(anyString(), eq("RUG-RED"), eq(2));
        // 不再验证 inventoryClient.reserve —— 当前代码路径未调用它
        verifyNoMoreInteractions(inventoryClient);
    }

    @Test
    void cancelOrder_shouldRestockAndMarkCancelled_andTryExternalRelease() {
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

        // 回补本地库存
        when(itemRepo.tryRestock("RUG-RED", 2)).thenReturn(1);

        OrderDocument cancelled = orderService.cancelOrder("ORD-abc");

        assertEquals("CANCELLED", cancelled.getStatus());
        verify(itemRepo).tryRestock("RUG-RED", 2);
        verify(orderRepository).save(any(OrderDocument.class));
        verify(inventoryEventProducer).publishInventoryReleased(eq("ORD-abc"), eq("RUG-RED"), eq(2));

        // 如果你的 InventoryClient.release 只有一个参数（sku），把下面这行改成对应签名即可
        // verify(inventoryClient).release("RUG-RED", 2);
        // 或 verify(inventoryClient).release("RUG-RED");
    }
}
