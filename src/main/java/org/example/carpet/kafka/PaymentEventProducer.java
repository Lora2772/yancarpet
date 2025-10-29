package org.example.carpet.kafka;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes payment-related events.
 * After a payment succeeds, we notify downstream consumers
 * (e.g. Order Service) that the order should move to PAID.
 */
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC_PAYMENT = "payment.succeeded";

    public void publishPaymentSucceeded(String orderId, double amountUsd) {
        String payload = """
            {
              "type": "PaymentSucceeded",
              "orderId": "%s",
              "amountUsd": %.2f
            }
            """.formatted(orderId, amountUsd);

        log.info("Publishing to {}: {}", TOPIC_PAYMENT, payload);
        kafkaTemplate.send(TOPIC_PAYMENT, orderId, payload);
    }
}
