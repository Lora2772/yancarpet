package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.repository.mongo.ItemDocumentRepository;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Handles stock availability and shipping promise.
 * Uses MongoDB (ItemDocumentRepository) for atomic inventory operations.
 * Cassandra for reservation tracking with TTL.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ItemDocumentRepository itemRepository;

    // === Cassandra: 使用 CassandraTemplate 直写预留（行级 TTL） ===
    private final CassandraTemplate cassandraTemplate;

    // 查询库存 + 返回承诺运输时间
    public InventoryStatus checkInventory(String sku) {
        var item = itemRepository.findBySku(sku);
        int qty = item.map(i -> i.getStockQuantity() != null ? i.getStockQuantity() : 0).orElse(0);
        return InventoryStatus.builder()
                .sku(sku)
                .availableQuantity(qty)
                .estimatedDeliveryBusinessDays(15)
                .estimatedDeliveryText("approximately 15 business days")
                .notes("Ships via sea freight from our overseas warehouse.")
                .build();
    }

    // 锁库存（用于创建订单）- 使用 MongoDB 原子操作
    public boolean reserve(String sku, int quantity) {
        int result = itemRepository.tryDeduct(sku, quantity);
        return result == 1;
    }

    // 释放库存（订单取消 / 支付失败）- 使用 MongoDB 原子操作
    public boolean release(String sku, int quantity) {
        int result = itemRepository.tryRestock(sku, quantity);
        return result == 1;
    }

    // ----------------------------------------------------------------------
    // Cassandra 预留记录（短寿命、写多读少；TTL 到期自动过期）
    // ----------------------------------------------------------------------

    /**
     * Cassandra（预留记录，双写两张查询模型表）:
     *  - inventory_reservations_by_sku (sku, reserved_at_ts DESC, order_id, qty) USING TTL ?
     *  - inventory_reservations_by_order (order_id, reserved_at_ts DESC, sku, qty) USING TTL ?
     */
    public void recordReservationCassandra(String orderId, String sku, int qty, Duration ttl) {
        long now = System.currentTimeMillis();
        int ttlSec = (int) Math.max(1, ttl.getSeconds());

        // by_sku
        cassandraTemplate.getCqlOperations().execute(
                "INSERT INTO inventory_reservations_by_sku (sku, reserved_at_ts, order_id, qty) " +
                        "VALUES (?, ?, ?, ?) USING TTL ?",
                sku, now, orderId, qty, ttlSec
        );

        // by_order
        cassandraTemplate.getCqlOperations().execute(
                "INSERT INTO inventory_reservations_by_order (order_id, reserved_at_ts, sku, qty) " +
                        "VALUES (?, ?, ?, ?) USING TTL ?",
                orderId, now, sku, qty, ttlSec
        );
    }

    /**
     * （可选）Cassandra：按主键删除一条预留（一般不需要，交由 TTL 过期）
     */
    public void deleteReservationCassandraBySku(String sku, long reservedAtTs) {
        cassandraTemplate.getCqlOperations().execute(
                "DELETE FROM inventory_reservations_by_sku WHERE sku = ? AND reserved_at_ts = ?",
                sku, reservedAtTs
        );
    }

    // 内部使用的结构体（不放 dto 包是因为这是 service <-> controller 的中间数据）
    @lombok.Data
    @lombok.Builder
    public static class InventoryStatus {
        private String sku;
        private int availableQuantity;
        private int estimatedDeliveryBusinessDays;
        private String estimatedDeliveryText;
        private String notes;
    }
}
