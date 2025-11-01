package org.example.carpet.model;

import java.util.Map;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/** 物流事件（追加式） */
@Table("shipment_events")
public class ShipmentEvent {

    @PrimaryKey
    private ShipmentEventKey key;

    @Column("tracking_id")
    private String trackingId;

    @Column("event_type")
    private String eventType;

    @Column("status")
    private String status;

    @Column("location")
    private String location;

    @Column("attributes")
    private Map<String, String> attributes;

    public ShipmentEvent() {}

    public ShipmentEvent(ShipmentEventKey key, String trackingId, String eventType,
                         String status, String location, Map<String, String> attributes) {
        this.key = key; this.trackingId = trackingId; this.eventType = eventType;
        this.status = status; this.location = location; this.attributes = attributes;
    }

    public ShipmentEventKey getKey() { return key; }
    public void setKey(ShipmentEventKey key) { this.key = key; }
    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
}
