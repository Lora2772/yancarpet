package org.example.carpet.cassandra.keys;

import java.io.Serializable;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;


@PrimaryKeyClass
public class InventoryReservationBySkuKey implements Serializable {

    @PrimaryKeyColumn(name = "sku", type = PrimaryKeyType.PARTITIONED)
    private String sku;

    @PrimaryKeyColumn(name = "reserved_at_ts", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Long reservedAtTs;

    public InventoryReservationBySkuKey() {}
    public InventoryReservationBySkuKey(String sku, Long reservedAtTs) {
        this.sku = sku;
        this.reservedAtTs = reservedAtTs;
    }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Long getReservedAtTs() { return reservedAtTs; }
    public void setReservedAtTs(Long reservedAtTs) { this.reservedAtTs = reservedAtTs; }
}
