package org.example.carpet.kafka;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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

    private static final String TOPIC_RESERVED = "inventory.reserved";
    private static final String TOPIC_RELEASED = "inventory.released";

    public void publishInventoryReserved(String orderId, String sku, int quantity) {
        String payload = """
            {
              "type": "InventoryReserved",
              "orderId": "%s",
              "sku": "%s",
              "quantity": %d
            }
            """.formatted(orderId, sku, quantity);

        log.info("Publishing to {}: {}", TOPIC_RESERVED, payload);
        kafkaTemplate.send(TOPIC_RESERVED, orderId, payload);
    }

    public void publishInventoryReleased(String orderId, String sku, int quantity) {
        String payload = """
            {
              "type": "InventoryReleased",
              "orderId": "%s",
              "sku": "%s",
              "quantity": %d
            }
            """.formatted(orderId, sku, quantity);

        log.info("Publishing to {}: {}", TOPIC_RELEASED, payload);
        kafkaTemplate.send(TOPIC_RELEASED, orderId, payload);
    }
}
