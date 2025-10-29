package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.kafka.PaymentEventProducer;
import org.example.carpet.model.PaymentRecord;
import org.example.carpet.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Handles payment flow.
 * Right now we simulate instant card success.
 * Future: support WeChat / Alipay QR with PENDING -> SUCCESS.
 */

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final PaymentEventProducer paymentEventProducer; // Kafka producer

    public PaymentRecord submitPayment(String orderId, String paymentMethod, double amount) {

        PaymentRecord record = PaymentRecord.builder()
                .id(null)
                .orderId(orderId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .completedAt(null)
                .build();

        record = paymentRepository.save(record);

        // Simulate instant success for CARD
        if ("CARD".equalsIgnoreCase(paymentMethod)) {
            record.setStatus("SUCCESS");
            record.setCompletedAt(LocalDateTime.now());
            record = paymentRepository.save(record);

            // update order status -> PAID
            orderService.markPaid(orderId);

            // emit payment success event to Kafka
            paymentEventProducer.publishPaymentSucceeded(orderId, amount);
        }

        return record;
    }

    public PaymentRecord getPaymentStatus(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("No payment for order: " + orderId));
    }
}
