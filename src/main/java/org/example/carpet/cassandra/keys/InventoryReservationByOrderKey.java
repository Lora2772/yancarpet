package org.example.carpet.cassandra.keys;

import java.io.Serializable;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

@PrimaryKeyClass
public class InventoryReservationByOrderKey implements Serializable {

    @PrimaryKeyColumn(name = "order_id", type = PrimaryKeyType.PARTITIONED)
    private String orderId;

    @PrimaryKeyColumn(name = "reserved_at_ts", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Long reservedAtTs;

    public InventoryReservationByOrderKey() {}
    public InventoryReservationByOrderKey(String orderId, Long reservedAtTs) {
        this.orderId = orderId;
        this.reservedAtTs = reservedAtTs;
    }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getReservedAtTs() { return reservedAtTs; }
    public void setReservedAtTs(Long reservedAtTs) { this.reservedAtTs = reservedAtTs; }
}
