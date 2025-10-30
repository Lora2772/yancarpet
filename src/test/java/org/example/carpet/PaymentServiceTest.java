package org.example.carpet.service;

import org.example.carpet.kafka.PaymentEventProducer;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.PaymentRecord;
import org.example.carpet.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests payment flow:
 * - submitPayment(): creates record, marks SUCCESS for CARD, marks order PAID, emits event
 * - refundPayment(): creates refund record, marks order REFUNDED
 * - getPaymentStatus(): reads from repo
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    OrderService orderService;

    @Mock
    PaymentEventProducer paymentEventProducer;

    @InjectMocks
    PaymentService paymentService;

    @Test
    void submitPayment_cardShouldMarkSuccessAndMarkOrderPaid() {
        // mock orderService.markPaid
        when(orderService.markPaid("ORD-123"))
                .thenReturn(
                        OrderDocument.builder()
                                .orderId("ORD-123")
                                .status("PAID")
                                .build()
                );

        // paymentRepository.save(...) should just echo back what we pass in
        when(paymentRepository.save(any(PaymentRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentRecord rec = paymentService.submitPayment(
                "ORD-123",
                "CARD",
                499.00
        );

        assertEquals("ORD-123", rec.getOrderId());
        assertEquals("CARD", rec.getPaymentMethod());
        assertEquals(499.00, rec.getAmount());
        assertEquals("SUCCESS", rec.getStatus());

        // verify PAID status update + event emission
        verify(orderService).markPaid("ORD-123");
        verify(paymentEventProducer).publishPaymentSucceeded("ORD-123", 499.00);
    }

    @Test
    void refundPayment_shouldCreateRefundRecordAndMarkOrderRefunded() {
        // Set up an existing successful payment
        PaymentRecord paidRecord = PaymentRecord.builder()
                .orderId("ORD-xyz")
                .amount(200.00)
                .paymentMethod("CARD")
                .status("SUCCESS")
                .build();

        when(paymentRepository.findByOrderId("ORD-xyz"))
                .thenReturn(Optional.of(paidRecord));

        // paymentRepository.save(...) echo back argument
        when(paymentRepository.save(any(PaymentRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Mock loading + saving order
        OrderDocument orderDoc = OrderDocument.builder()
                .orderId("ORD-xyz")
                .status("PAID")
                .build();

        when(orderService.getOrderByOrderId("ORD-xyz")).thenReturn(orderDoc);
        when(orderService.saveDirect(any(OrderDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentRecord refund = paymentService.refundPayment("ORD-xyz", "customer_cancel");

        assertEquals("ORD-xyz", refund.getOrderId());
        assertEquals("REFUND_SUCCESS", refund.getStatus());
        assertTrue(refund.getAmount() < 0, "refund amount should be negative");

        // After refund, order should be REFUNDED
        assertEquals("REFUNDED", orderDoc.getStatus());
        verify(orderService).saveDirect(orderDoc);
    }

    @Test
    void getPaymentStatus_shouldReturnRecordFromRepo() {
        PaymentRecord mock = PaymentRecord.builder()
                .orderId("ORD-abc")
                .status("SUCCESS")
                .paymentMethod("CARD")
                .amount(100.00)
                .build();

        when(paymentRepository.findByOrderId("ORD-abc"))
                .thenReturn(Optional.of(mock));

        PaymentRecord out = paymentService.getPaymentStatus("ORD-abc");

        assertEquals("ORD-abc", out.getOrderId());
        assertEquals("SUCCESS", out.getStatus());
        assertEquals("CARD", out.getPaymentMethod());
    }
}
