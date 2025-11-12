package org.example.carpet.kafka;

import lombok.RequiredArgsConstructor;
import org.example.carpet.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for payment events.
 * Listens for payment.succeeded events and updates the order status to PAID.
 *
 * Note:
 * This consumer works alongside the synchronous call in PaymentService.
 * The OrderService.markPaid() method is idempotent, so duplicate calls are safe.
 * This provides both immediate consistency (sync) and resilience (async).
 */
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderService orderService;

    @KafkaListener(topics = "payment.succeeded", groupId = "carpet-group")
    public void handlePaymentSucceeded(String messageJson) {
        log.info("Received Kafka event payment.succeeded: {}", messageJson);

        // super simple parsing just to demo the flow
        // messageJson was published as:
        // {
        //   "type": "PaymentSucceeded",
        //   "orderId": "ORD-abc123",
        //   "amountUsd": 499.00
        // }

        String orderId = extractOrderId(messageJson);
        if (orderId != null) {
            try {
                orderService.markPaid(orderId);
                log.info("Order {} marked as PAID via async consumer", orderId);
            } catch (Exception e) {
                log.warn("Couldn't mark order {} paid from Kafka consumer: {}", orderId, e.getMessage());
            }
        }
    }

    // quick & dirty JSON extraction just for demo
    private String extractOrderId(String json) {
        // naive parse: find "orderId":"...".
        String key = "\"orderId\":";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int startQuote = json.indexOf("\"", idx + key.length());
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (startQuote == -1 || endQuote == -1) return null;
        return json.substring(startQuote + 1, endQuote);
    }
}
