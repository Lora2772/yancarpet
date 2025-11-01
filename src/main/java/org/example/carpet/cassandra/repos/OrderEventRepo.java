package org.example.carpet.cassandra.repos;

import java.util.List;
import org.example.carpet.cassandra.entities.OrderEvent;
import org.example.carpet.cassandra.keys.OrderEventKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderEventRepo extends CassandraRepository<OrderEvent, OrderEventKey> {

    @Query("SELECT * FROM order_events_by_order WHERE order_id = :orderId LIMIT :limit")
    List<OrderEvent> findRecentEvents(@Param("orderId") String orderId, @Param("limit") int limit);
}
