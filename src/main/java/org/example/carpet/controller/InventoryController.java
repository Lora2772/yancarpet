package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.service.InventoryService;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 库存和交付时效相关接口
 *
 * - GET  /inventory/check?sku=RUG-12345         可售数量 + 运输承诺
 * - POST /inventory/reserve?sku=...&quantity=.. 扣减(本地)
 * - POST /inventory/release?sku=...&quantity=.. 回补(本地)
 *
 * 下面三个是 Cassandra 相关的辅助接口，便于课堂演示/自测：
 * - POST /inventory/reservations/record         // Cassandra 记录一条预留（行级 TTL）
 * - GET  /inventory/reservations/by-sku         // Cassandra 按 SKU 查看最近预留
 * - GET  /inventory/reservations/by-order       // Cassandra 按订单查看预留
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // 为了少改 Service，这里直接注入 Template 用于查询预留列表（写入仍走 Service）
    private final CassandraTemplate cassandraTemplate;

    @GetMapping("/check")
    public InventoryService.InventoryStatus check(@RequestParam String sku) {
        return inventoryService.checkInventory(sku);
    }

    @PostMapping("/reserve")
    public boolean reserve(@RequestParam String sku, @RequestParam int quantity) {
        return inventoryService.reserve(sku, quantity);
    }

    @PostMapping("/release")
    public void release(@RequestParam String sku, @RequestParam int quantity) {
        inventoryService.release(sku, quantity);
    }

    // -------------------- Cassandra：预留辅助接口 --------------------

    /** Cassandra：手动写一条预留（行级 TTL，默认 15 分钟），用于测试/运维 */
    @PostMapping("/reservations/record")
    public Map<String, Object> recordReservationCassandra(
            @RequestParam String orderId,
            @RequestParam String sku,
            @RequestParam int qty,
            @RequestParam(required = false, defaultValue = "15") int ttlMinutes
    ) {
        inventoryService.recordReservationCassandra(orderId, sku, qty, Duration.ofMinutes(ttlMinutes));
        return Map.of("ok", true, "orderId", orderId, "sku", sku, "qty", qty, "ttlMinutes", ttlMinutes);
    }

    /** Cassandra：按 SKU 查看最近 N 条预留（用于演示）。表：inventory_reservations_by_sku */
    @GetMapping("/reservations/by-sku")
    public List<Map<String, Object>> listReservationsBySku(
            @RequestParam String sku,
            @RequestParam(required = false, defaultValue = "20") int limit
    ) {
        String cql = "SELECT sku, reserved_at_ts, order_id, qty FROM inventory_reservations_by_sku " +
                "WHERE sku = ? LIMIT " + Math.max(1, limit);
        return cassandraTemplate.getCqlOperations().query(cql, ps -> ps.bind(sku), (row, i) -> Map.of(
                "sku", row.getString("sku"),
                "reservedAtTs", row.getLong("reserved_at_ts"),
                "orderId", row.getString("order_id"),
                "qty", row.getInt("qty")
        ));
    }

    /** Cassandra：按订单查看最近 N 条预留（用于演示）。表：inventory_reservations_by_order */
    @GetMapping("/reservations/by-order")
    public List<Map<String, Object>> listReservationsByOrder(
            @RequestParam String orderId,
            @RequestParam(required = false, defaultValue = "20") int limit
    ) {
        String cql = "SELECT order_id, reserved_at_ts, sku, qty FROM inventory_reservations_by_order " +
                "WHERE order_id = ? LIMIT " + Math.max(1, limit);
        return cassandraTemplate.getCqlOperations().query(cql, ps -> ps.bind(orderId), (row, i) -> Map.of(
                "orderId", row.getString("order_id"),
                "reservedAtTs", row.getLong("reserved_at_ts"),
                "sku", row.getString("sku"),
                "qty", row.getInt("qty")
        ));
    }
}
