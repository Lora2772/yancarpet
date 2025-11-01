package org.example.carpet.repository.cassandra;

import java.time.Instant;
import java.util.List;

import org.example.carpet.model.ShipmentEvent;
import org.example.carpet.model.ShipmentEventKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

/**
 * Cassandra 仓库：存取物流事件
 */
@Repository
public interface ShipmentEventRepository extends CassandraRepository<ShipmentEvent, ShipmentEventKey> {

    // 按订单ID查
    List<ShipmentEvent> findAllByKeyOrderId(String orderId);

    // 在分区内按时间范围查
    List<ShipmentEvent> findAllByKeyOrderIdAndKeyTsBetween(String orderId, Instant start, Instant end);

    // 按追踪号查
    List<ShipmentEvent> findAllByTrackingId(String trackingId);

    // 按事件类型查（仅当 event_type 建索引）
    List<ShipmentEvent> findAllByKeyOrderIdAndEventType(String orderId, String eventType);
}
