package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.client.InventoryClient;
import org.example.carpet.exception.InsufficientStockException;
import org.example.carpet.kafka.InventoryEventProducer;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.OrderLineItem;
import org.example.carpet.repository.mongo.ItemDocumentRepository;
import org.example.carpet.repository.mongo.OrderRepository;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Duration;
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
 *
 * Cassandra 集成：
 * - 订单事件时间线：order_events_by_order (order_id, ts DESC, type, payload_json)
 * - 预留记录：委托 InventoryService.recordReservationCassandra(...)
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;                 // Mongo: orders
    private final ItemDocumentRepository itemRepo;                 // Mongo: items (tryDeduct/tryRestock)
    private final InventoryEventProducer inventoryEventProducer;   // Kafka events
    private final InventoryClient inventoryClient;                 // 可选的外部库存服务（best-effort）

    // === Cassandra: 事件时间线 ===
    private final CassandraTemplate cassandraTemplate;
    // === 复用已有库存服务，在其中写 Cassandra 预留 ===
    private final InventoryService inventoryService;

    // ====== 新增：查询订单历史（分页，按 createdAt 倒序） ======
    public Page<OrderDocument> getOrderHistory(String customerEmail, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100); // 防止一次拉太多
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findByCustomerEmail(customerEmail, pageable);
    }

    /**
     * 创建订单：逐条原子扣减库存，任何一条不足则回补已扣部分并抛 409 业务异常
     * Cassandra：
     *  - 追加事件：OrderCreated / InventoryReserved
     *  - 记录预留：inventory_reservations_*（TTL 15m）
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

        // --- Cassandra: 事件时间线 ---
        appendOrderEvent(orderId, "OrderCreated", "{\"email\":\"" + customerEmail + "\"}");

        // 6) 发送库存预留事件（每一行） + Cassandra 侧预留记录 + 事件
        for (OrderLineItem line : items) {
            // Kafka（原有）
            try { inventoryEventProducer.publishInventoryReserved(orderId, line.getSku(), line.getQuantity()); }
            catch (Exception ignore) {}

            // Cassandra 预留（TTL 15 分钟）
            try { inventoryService.recordReservationCassandra(orderId, line.getSku(), line.getQuantity(), Duration.ofMinutes(15)); }
            catch (Exception ignore) {}

            // Cassandra 事件
            appendOrderEvent(orderId, "InventoryReserved",
                    "{\"sku\":\""+line.getSku()+"\",\"qty\":"+line.getQuantity()+"}");
        }

        return saved;
    }

    public OrderDocument getOrderByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    /**
     * 取消：回补 Mongo 库存；外部库存服务 best-effort 释放；发 Release 事件；Cassandra 写事件
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
        }

        // Cassandra 事件
        appendOrderEvent(orderId, "InventoryReleased", "{}");

        // Kafka 释放事件（原有）
        for (OrderLineItem line : order.getItems()) {
            try { inventoryEventProducer.publishInventoryReleased(orderId, line.getSku(), line.getQuantity()); }
            catch (Exception ignore) {}
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

        // Cassandra 事件
        appendOrderEvent(orderId, "PaymentSucceeded", "{}");

        return orderRepository.save(order);
    }

    /** 供其它服务直接更新订单状态 */
    public OrderDocument saveDirect(OrderDocument order) {
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    // ----------------------------------------------------------------------
    // Cassandra：订单事件时间线（order_events_by_order）
    // ----------------------------------------------------------------------
    private void appendOrderEvent(String orderId, String type, String payloadJson) {
        long now = System.currentTimeMillis();
        String payload = (payloadJson == null || payloadJson.isBlank()) ? "{}" : payloadJson;
        try {
            cassandraTemplate.getCqlOperations().execute(
                    "INSERT INTO order_events_by_order (order_id, ts, type, payload_json) VALUES (?, ?, ?, ?)",
                    orderId, now, type, payload
            );
        } catch (Exception ignore) {
            // 事件失败不回滚主交易
        }
    }
}
