package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.exception.InvalidOrderStateException;
import org.example.carpet.exception.PaymentNotFoundException;
import org.example.carpet.kafka.PaymentEventProducer;
import org.example.carpet.ledger.PaymentLedgerEntity;
import org.example.carpet.repository.jpa.PaymentLedgerRepository;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.PaymentRecord;
import org.example.carpet.repository.mongo.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles payment lifecycle for an order:
 *  - submitPayment: capture funds
 *  - refundPayment: reverse funds
 *
 * NOTE:
 *  - This service writes Mongo (PaymentRecord / OrderDocument) and Postgres (PaymentLedgerEntity).
 *  - For cross-store reliability in production, consider the Outbox/Saga pattern for Kafka events.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;               // Mongo: payment records
    private final OrderService orderService;                         // Mongo: orders
    private final PaymentEventProducer paymentEventProducer;         // Kafka events
    private final PaymentLedgerRepository paymentLedgerRepository;   // JPA: immutable ledger (Postgres)

    /**
     * User clicks "Pay Now".
     * Flow:
     *  1) create PENDING record
     *  2) (demo) auto-approve CARD -> SUCCESS
     *  3) mark order PAID
     *  4) emit payment.succeeded event
     *  5) append immutable ledger row
     */
    @Transactional
    public PaymentRecord submitPayment(String orderId, String paymentMethod, double amount) {
        // 1) create pending record
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

        // 2) demo: auto-approve CARD
        if ("CARD".equalsIgnoreCase(paymentMethod)) {
            record.setStatus("SUCCESS");
            record.setCompletedAt(LocalDateTime.now());
            record = paymentRepository.save(record);

            // 3) mark order PAID
            orderService.markPaid(orderId);

            // 4) async event (failure shouldn't roll back DB here; add outbox in prod)
            try {
                paymentEventProducer.publishPaymentSucceeded(orderId, amount);
            } catch (Exception ignored) { /* log.warn("publishPaymentSucceeded failed", ignored); */ }

            // 5) immutable ledger
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
     * Flow:
     *  1) ensure there was a successful payment
     *  2) create negative refund record (REFUND_SUCCESS)
     *  3) mark order REFUNDED
     *  4) append immutable ledger row (negative amount)
     */
    @Transactional
    public PaymentRecord refundPayment(String orderId, String reason) {
        // 1) find successful payment
        PaymentRecord paid = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));

        if (!"SUCCESS".equalsIgnoreCase(paid.getStatus())) {
            throw new InvalidOrderStateException(orderId, paid.getStatus(), "SUCCESS");
        }

        double refundAmount = paid.getAmount();

        // 2) create refund record (negative amount)
        PaymentRecord refundRecord = PaymentRecord.builder()
                .id(null)
                .orderId(orderId)
                .amount(refundAmount * -1)  // negative = back to customer
                .paymentMethod(paid.getPaymentMethod())
                .status("REFUND_SUCCESS")
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        refundRecord = paymentRepository.save(refundRecord);

        // 3) mark order as REFUNDED
        OrderDocument order = orderService.getOrderByOrderId(orderId);
        order.setStatus("REFUNDED"); // or CANCELLED_REFUNDED per business wording
        orderService.saveDirect(order);

        // (Optional) emit refund event
        // try { paymentEventProducer.publishPaymentRefunded(orderId, refundAmount); } catch (Exception ignored) {}

        // 4) immutable refund ledger (negative)
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
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }

    /**
     * Update an existing payment record.
     * Allows updating status, payment method, or amount.
     * If status changes to SUCCESS, marks the order as PAID and creates ledger entry.
     *
     * @param orderId The order ID to update payment for
     * @param newStatus Optional new status (PENDING, SUCCESS, FAILED)
     * @param newPaymentMethod Optional new payment method
     * @param newAmount Optional new amount
     * @return Updated payment record
     */
    @Transactional
    public PaymentRecord updatePayment(String orderId, String newStatus, String newPaymentMethod, Double newAmount) {
        // Find existing payment
        PaymentRecord payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));

        String oldStatus = payment.getStatus();
        boolean statusChanged = false;

        // Update fields if provided
        if (newStatus != null && !newStatus.isBlank()) {
            payment.setStatus(newStatus);
            statusChanged = !newStatus.equalsIgnoreCase(oldStatus);
        }

        if (newPaymentMethod != null && !newPaymentMethod.isBlank()) {
            payment.setPaymentMethod(newPaymentMethod);
        }

        if (newAmount != null && newAmount > 0) {
            payment.setAmount(newAmount);
        }

        // If status changed to SUCCESS, complete the payment flow
        if (statusChanged && "SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            payment.setCompletedAt(LocalDateTime.now());

            // Mark order as PAID
            orderService.markPaid(orderId);

            // Emit payment succeeded event
            try {
                paymentEventProducer.publishPaymentSucceeded(orderId, payment.getAmount());
            } catch (Exception ignored) { /* log warning in production */ }

            // Create immutable ledger entry
            paymentLedgerRepository.save(
                    PaymentLedgerEntity.builder()
                            .orderId(orderId)
                            .amountUsd(payment.getAmount())
                            .paymentMethod(payment.getPaymentMethod())
                            .status("SUCCESS")
                            .recordedAt(LocalDateTime.now())
                            .build()
            );
        }

        // If status changed to FAILED, set completed timestamp
        if (statusChanged && "FAILED".equalsIgnoreCase(payment.getStatus())) {
            payment.setCompletedAt(LocalDateTime.now());
        }

        return paymentRepository.save(payment);
    }
}
