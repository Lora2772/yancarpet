package org.example.carpet.cassandra.repos;

import java.util.List;
import org.example.carpet.cassandra.entities.InventoryReservationBySku;
import org.example.carpet.cassandra.keys.InventoryReservationBySkuKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationBySkuRepo
        extends CassandraRepository<InventoryReservationBySku, InventoryReservationBySkuKey> {

    @Query("SELECT * FROM inventory_reservations_by_sku WHERE sku = :sku LIMIT :limit")
    List<InventoryReservationBySku> findRecentBySku(@Param("sku") String sku, @Param("limit") int limit);
}
