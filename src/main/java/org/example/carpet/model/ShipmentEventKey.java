package org.example.carpet.model;

import java.io.Serializable;
import java.time.Instant;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

/**
 * Cassandra 物流事件的复合主键
 * 分区键：orderId
 * 聚簇键：leg + ts
 */
@PrimaryKeyClass
public class ShipmentEventKey implements Serializable {

    @PrimaryKeyColumn(name = "order_id", type = PrimaryKeyType.PARTITIONED)
    private String orderId;

    @PrimaryKeyColumn(name = "leg", ordinal = 0, type = PrimaryKeyType.CLUSTERED)
    private Integer leg;

    @PrimaryKeyColumn(name = "ts", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private Instant ts;

    public ShipmentEventKey() {}

    public ShipmentEventKey(String orderId, Integer leg, Instant ts) {
        this.orderId = orderId;
        this.leg = leg;
        this.ts = ts;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Integer getLeg() { return leg; }
    public void setLeg(Integer leg) { this.leg = leg; }

    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
}
