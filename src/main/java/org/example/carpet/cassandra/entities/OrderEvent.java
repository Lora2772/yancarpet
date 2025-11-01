package org.example.carpet.cassandra.entities;

import org.example.carpet.cassandra.keys.OrderEventKey;
import org.springframework.data.cassandra.core.mapping.*;

@Table("order_events_by_order")
public class OrderEvent {

    @PrimaryKey
    private OrderEventKey key;

    @Column("type")
    private String type;

    @Column("payload_json")
    private String payloadJson;

    public OrderEvent() {}
    public OrderEvent(OrderEventKey key, String type, String payloadJson) {
        this.key = key; this.type = type; this.payloadJson = payloadJson;
    }
    public OrderEventKey getKey() { return key; }
    public void setKey(OrderEventKey key) { this.key = key; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
}
