package org.example.carpet.cassandra.keys;

import java.io.Serializable;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;


@PrimaryKeyClass
public class OrderEventKey implements Serializable {

    @PrimaryKeyColumn(name = "order_id", type = PrimaryKeyType.PARTITIONED)
    private String orderId;

    @PrimaryKeyColumn(name = "ts", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Long ts;

    public OrderEventKey() {}
    public OrderEventKey(String orderId, Long ts) { this.orderId = orderId; this.ts = ts; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getTs() { return ts; }
    public void setTs(Long ts) { this.ts = ts; }
}
