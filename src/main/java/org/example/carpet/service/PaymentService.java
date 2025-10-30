package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.kafka.PaymentEventProducer;
import org.example.carpet.ledger.PaymentLedgerEntity;
import org.example.carpet.ledger.PaymentLedgerRepository;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.PaymentRecord;
import org.example.carpet.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles payment lifecycle for an order:
 * - submitPayment: capture funds
 * - refundPayment: reverse funds
 *
 * We annotate both flows with @Transactional so that updating the PaymentRecord
 * and updating the Order status happen in one (service-layer) transaction.
 * In production you'd also use an outbox pattern to guarantee Kafka delivery.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentLedgerRepository paymentLedgerRepository;


    /**
     * User clicks "Pay Now".
     * 1. create a PENDING payment record
     * 2. simulate card success -> mark SUCCESS
     * 3. mark order PAID
     * 4. emit payment.succeeded event
     */
    @Transactional
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

        // For CARD we auto-approve in this demo
        if ("CARD".equalsIgnoreCase(paymentMethod)) {
            record.setStatus("SUCCESS");
            record.setCompletedAt(LocalDateTime.now());
            record = paymentRepository.save(record);

            // update order status -> PAID
            orderService.markPaid(orderId);

            // publish async event
            paymentEventProducer.publishPaymentSucceeded(orderId, amount);

            paymentLedgerRepository.save(
                    PaymentLedgerEntity.builder()
                            .orderId(orderId)
                            .amountUsd(amount)
                            .paymentMethod(paymentMethod)
                            .status("SUCCESS")
                            .recordedAt(LocalDateTime.now())
                            .build()
            );

        }

        return record;
    }

    /**
     * Reverse Payment / Refund API.
     * Called when a paid order is cancelled or customer is refunded.
     * 1. ensure there was a successful payment
     * 2. create a negative refund record with REFUND_SUCCESS
     * 3. mark order REFUNDED
     */
    @Transactional
    public PaymentRecord refundPayment(String orderId, String reason) {

        PaymentRecord paid = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("No payment found for order " + orderId));

        if (!"SUCCESS".equalsIgnoreCase(paid.getStatus())) {
            throw new RuntimeException("Payment not settled, cannot refund.");
        }

        double refundAmount = paid.getAmount();

        PaymentRecord refundRecord = PaymentRecord.builder()
                .id(null)
                .orderId(orderId)
                .amount(refundAmount * -1)  // negative means money going back to customer
                .paymentMethod(paid.getPaymentMethod())
                .status("REFUND_SUCCESS")
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        refundRecord = paymentRepository.save(refundRecord);

        // mark the order as REFUNDED
        OrderDocument order = orderService.getOrderByOrderId(orderId);
        order.setStatus("REFUNDED"); // We can describe this as CANCELLED_REFUNDED in docs if needed
        orderService.saveDirect(order);

        // (OPTIONAL) emit refund event via Kafka outbox pattern
        // paymentEventProducer.publishPaymentRefunded(orderId, refundAmount);

        paymentLedgerRepository.save(
                PaymentLedgerEntity.builder()
                        .orderId(orderId)
                        .amountUsd(refundAmount * -1)
                        .paymentMethod(paid.getPaymentMethod())
                        .status("REFUND_SUCCESS")
                        .recordedAt(LocalDateTime.now())
                        .build()
        );

        return refundRecord;
    }

    public PaymentRecord getPaymentStatus(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("No payment for order: " + orderId));
    }
}
