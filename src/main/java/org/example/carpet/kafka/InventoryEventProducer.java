package org.example.carpet.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes inventory-related events.
 * In real microservices, Inventory Service would emit these
 * after reserve/release operations.
 */
@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper(); // 生成 JSON，避免模板重复

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    public enum EventType {
        RESERVED("InventoryReserved", "inventory.reserved"),
        RELEASED("InventoryReleased", "inventory.released");

        public final String typeName;
        public final String topic;

        EventType(String typeName, String topic) {
            this.typeName = typeName;
            this.topic = topic;
        }
    }

    public void publishInventoryReserved(String orderId, String sku, int quantity) {
        publish(EventType.RESERVED, orderId, sku, quantity);
    }

    public void publishInventoryReleased(String orderId, String sku, int quantity) {
        publish(EventType.RELEASED, orderId, sku, quantity);
    }

    // ---- 通用方法，消除重复 ----
    private void publish(EventType eventType, String orderId, String sku, int quantity) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "type", eventType.typeName,
                    "orderId", orderId,
                    "sku", sku,
                    "quantity", quantity
            ));
        } catch (Exception e) {
            log.error("Failed to serialize {} event (orderId={}, sku={}, qty={})",
                    eventType.typeName, orderId, sku, quantity, e);
            return;
        }

        if (!kafkaEnabled) {
            log.info("[DEV-NO-KAFKA] {} -> topic={} key={} payload={}",
                    eventType.typeName, eventType.topic, orderId, payload);
            return;
        }

        log.info("Publishing {} -> topic={} key={}", eventType.typeName, eventType.topic, orderId);
        kafkaTemplate.send(eventType.topic, orderId, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka publish failed: topic={} key={} type={}: {}",
                                eventType.topic, orderId, eventType.typeName, ex.toString());
                    } else if (result != null && result.getRecordMetadata() != null) {
                        var md = result.getRecordMetadata();
                        log.debug("Kafka published: topic={} partition={} offset={} key={}",
                                md.topic(), md.partition(), md.offset(), orderId);
                    }
                });
    }
}
