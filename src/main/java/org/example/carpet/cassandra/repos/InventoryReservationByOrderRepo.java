package org.example.carpet.cassandra.repos;

import java.util.List;
import org.example.carpet.cassandra.entities.InventoryReservationByOrder;
import org.example.carpet.cassandra.keys.InventoryReservationByOrderKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationByOrderRepo
        extends CassandraRepository<InventoryReservationByOrder, InventoryReservationByOrderKey> {

    @Query("SELECT * FROM inventory_reservations_by_order WHERE order_id = :orderId LIMIT :limit")
    List<InventoryReservationByOrder> findByOrderId(@Param("orderId") String orderId, @Param("limit") int limit);
}
