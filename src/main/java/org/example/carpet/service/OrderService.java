package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.client.InventoryClient;
import org.example.carpet.exception.InsufficientStockException;
import org.example.carpet.kafka.InventoryEventProducer;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.OrderLineItem;
import org.example.carpet.repository.mongo.ItemDocumentRepository;
import org.example.carpet.repository.mongo.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order lifecycle:
 * - create (reserve = atomic deduct in Mongo)
 * - get
 * - cancel (restock in Mongo; best-effort external release)
 * - markPaid
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;                 // Mongo: orders
    private final ItemDocumentRepository itemRepo;                 // Mongo: items (tryDeduct/tryRestock)
    private final InventoryEventProducer inventoryEventProducer;   // Kafka events
    private final InventoryClient inventoryClient;                 // 可选的外部库存服务（best-effort）

    /**
     * 创建订单：逐条原子扣减库存，任何一条不足则回补已扣部分并抛 409 业务异常
     */
    public OrderDocument createOrder(String customerEmail, List<OrderLineItem> items) {
        // 1) 逐条扣减（原子条件：stockQuantity >= qty）
        List<OrderLineItem> deducted = new ArrayList<>();
        for (OrderLineItem line : items) {
            int ok = itemRepo.tryDeduct(line.getSku(), line.getQuantity());
            if (ok == 0) {
                // 回补之前成功扣减的
                for (OrderLineItem r : deducted) {
                    try { itemRepo.tryRestock(r.getSku(), r.getQuantity()); } catch (Exception ignore) {}
                }
                throw new InsufficientStockException(line.getSku(), line.getQuantity(), -1);
            }
            deducted.add(line);
        }

        // 2) 计算总价
        double total = items.stream()
                .mapToDouble(li -> li.getPrice() * li.getQuantity())
                .sum();

        // 3) 业务订单号
        String orderId = "ORD-" + UUID.randomUUID();

        // 4) 构造订单
        OrderDocument order = OrderDocument.builder()
                .orderId(orderId)
                .customerEmail(customerEmail)
                .items(items)
                .totalAmount(total)
                .status("RESERVED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 5) 持久化
        OrderDocument saved = orderRepository.save(order);

        // 6) 发送库存预留事件（每一行）
        for (OrderLineItem line : items) {
            try {
                inventoryEventProducer.publishInventoryReserved(orderId, line.getSku(), line.getQuantity());
            } catch (Exception e) {
                // 事件失败不回滚业务（可加重试/死信队列），仅记录日志
                // log.warn("failed to publish InventoryReserved", e);
            }
        }

        return saved;
    }

    public OrderDocument getOrderByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    /**
     * 取消：回补 Mongo 库存；外部库存服务 best-effort 释放；发 Release 事件
     */
    public OrderDocument cancelOrder(String orderId) {
        OrderDocument order = getOrderByOrderId(orderId);
        if (!"RESERVED".equals(order.getStatus()) && !"PAID".equals(order.getStatus())) {
            return order; // 其他状态不处理
        }

        for (OrderLineItem line : order.getItems()) {
            // 回补本地库存
            try { itemRepo.tryRestock(line.getSku(), line.getQuantity()); } catch (Exception ignore) {}

            // 可选：外部库存释放（失败不影响主流程）
            try { inventoryClient.release(line.getSku(), line.getQuantity()); } catch (Exception ignore) {}

            // 发送释放事件
            try {
                inventoryEventProducer.publishInventoryReleased(orderId, line.getSku(), line.getQuantity());
            } catch (Exception ignore) {}
        }

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public OrderDocument markPaid(String orderId) {
        OrderDocument order = getOrderByOrderId(orderId);
        if (!"RESERVED".equals(order.getStatus())) {
            throw new RuntimeException("Order not in RESERVED state, can't mark PAID.");
        }
        order.setStatus("PAID");
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    /** 供其它服务直接更新订单状态 */
    public OrderDocument saveDirect(OrderDocument order) {
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }
}
