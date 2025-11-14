package org.example.carpet.service;

import org.example.carpet.kafka.PaymentEventProducer;
import org.example.carpet.ledger.PaymentLedgerEntity;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.PaymentRecord;
import org.example.carpet.repository.jpa.PaymentLedgerRepository;
import org.example.carpet.repository.mongo.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests payment flow:
 * - submitPayment(): creates record, marks SUCCESS for CARD, marks order PAID, emits event, writes ledger
 * - refundPayment(): creates refund record, marks order REFUNDED, writes negative ledger
 * - getPaymentStatus(): reads from repo
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock OrderService orderService;
    @Mock PaymentEventProducer paymentEventProducer;
    @Mock PaymentLedgerRepository paymentLedgerRepository; // ✅ 关键：mock JPA 仓库，避免 NPE

    @InjectMocks PaymentService paymentService;

    @Test
    void submitPayment_cardShouldMarkSuccessAndMarkOrderPaid() {
        // orderService.markPaid 正常返回
        when(orderService.markPaid("ORD-123"))
                .thenReturn(OrderDocument.builder()
                        .orderId("ORD-123")
                        .status("PAID")
                        .build());

        // paymentRepository.save 回传入参
        when(paymentRepository.save(any(PaymentRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // ledger save 回传入参
        when(paymentLedgerRepository.save(any(PaymentLedgerEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentRecord rec = paymentService.submitPayment("ORD-123", "CARD", 499.00);

        assertEquals("ORD-123", rec.getOrderId());
        assertEquals("CARD", rec.getPaymentMethod());
        assertEquals(499.00, rec.getAmount(), 1e-6);
        assertEquals("SUCCESS", rec.getStatus());

        // 验证订单置为 PAID + 事件 + 账本写入
        verify(orderService).markPaid("ORD-123");
        verify(paymentEventProducer).publishPaymentSucceeded("ORD-123", 499.00);
        verify(paymentLedgerRepository).save(argThat(e ->
                "ORD-123".equals(e.getOrderId())
                        && e.getAmountUsd() == 499.00
                        && "SUCCESS".equals(e.getStatus())
        ));
    }

    @Test
    void refundPayment_shouldCreateRefundRecordAndMarkOrderRefunded() {
        // 现有成功支付
        PaymentRecord paidRecord = PaymentRecord.builder()
                .orderId("ORD-xyz")
                .amount(200.00)
                .paymentMethod("CARD")
                .status("SUCCESS")
                .build();
        when(paymentRepository.findByOrderId("ORD-xyz"))
                .thenReturn(Optional.of(paidRecord));

        // save echo
        when(paymentRepository.save(any(PaymentRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 加载与保存订单
        OrderDocument orderDoc = OrderDocument.builder()
                .orderId("ORD-xyz")
                .status("PAID")
                .build();
        when(orderService.getOrderByOrderId("ORD-xyz")).thenReturn(orderDoc);
        when(orderService.saveDirect(any(OrderDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(paymentLedgerRepository.save(any(PaymentLedgerEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentRecord refund = paymentService.refundPayment("ORD-xyz", "customer_cancel");

        assertEquals("ORD-xyz", refund.getOrderId());
        assertEquals("REFUND_SUCCESS", refund.getStatus());
        assertTrue(refund.getAmount() < 0, "refund amount should be negative");

        // 订单应为 REFUNDED
        assertEquals("REFUNDED", orderDoc.getStatus());
        verify(orderService).saveDirect(orderDoc);

        // 账本记录为负
        verify(paymentLedgerRepository).save(argThat(e ->
                "ORD-xyz".equals(e.getOrderId())
                        && e.getAmountUsd() == -200.00
                        && "REFUND_SUCCESS".equals(e.getStatus())
        ));
    }

    @Test
    void getPaymentStatus_shouldReturnRecordFromRepo() {
        PaymentRecord mockRec = PaymentRecord.builder()
                .orderId("ORD-abc")
                .status("SUCCESS")
                .paymentMethod("CARD")
                .amount(100.00)
                .build();
        when(paymentRepository.findByOrderId("ORD-abc"))
                .thenReturn(Optional.of(mockRec));

        PaymentRecord out = paymentService.getPaymentStatus("ORD-abc");

        assertEquals("ORD-abc", out.getOrderId());
        assertEquals("SUCCESS", out.getStatus());
        assertEquals("CARD", out.getPaymentMethod());
        assertEquals(100.00, out.getAmount(), 1e-6);
    }

    @Test
    void getPaymentStatus_notFound_shouldThrowException() {
        when(paymentRepository.findByOrderId("ORD-nonexistent"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            paymentService.getPaymentStatus("ORD-nonexistent");
        });
    }

    @Test
    void submitPayment_nonCardMethod_shouldCreatePending() {
        when(paymentRepository.save(any(PaymentRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentRecord rec = paymentService.submitPayment("ORD-456", "ALIPAY", 299.00);

        assertEquals("ORD-456", rec.getOrderId());
        assertEquals("ALIPAY", rec.getPaymentMethod());
        assertEquals("PENDING", rec.getStatus());

        // Should not mark order paid or emit events for pending payment
        verify(orderService, never()).markPaid(anyString());
        verify(paymentEventProducer, never()).publishPaymentSucceeded(anyString(), anyDouble());
        verify(paymentLedgerRepository, never()).save(any());
    }

    @Test
    void refundPayment_nonSuccessfulPayment_shouldThrowException() {
        PaymentRecord pendingRecord = PaymentRecord.builder()
                .orderId("ORD-pending")
                .status("PENDING")
                .build();

        when(paymentRepository.findByOrderId("ORD-pending"))
                .thenReturn(Optional.of(pendingRecord));

        assertThrows(RuntimeException.class, () -> {
            paymentService.refundPayment("ORD-pending", "reason");
        });
    }

    @Test
    void refundPayment_paymentNotFound_shouldThrowException() {
        when(paymentRepository.findByOrderId("ORD-notfound"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            paymentService.refundPayment("ORD-notfound", "reason");
        });
    }

    @Test
    void updatePayment_changeStatusToSuccess_shouldMarkOrderPaid() {
        PaymentRecord existing = PaymentRecord.builder()
                .orderId("ORD-update")
                .status("PENDING")
                .paymentMethod("WECHAT")
                .amount(150.00)
                .build();

        when(paymentRepository.findByOrderId("ORD-update"))
                .thenReturn(Optional.of(existing));

        when(paymentRepository.save(any(PaymentRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(orderService.markPaid("ORD-update"))
                .thenReturn(OrderDocument.builder().orderId("ORD-update").status("PAID").build());

        when(paymentLedgerRepository.save(any(PaymentLedgerEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentRecord updated = paymentService.updatePayment("ORD-update", "SUCCESS", null, null);

        assertEquals("SUCCESS", updated.getStatus());
        assertNotNull(updated.getCompletedAt());
        verify(orderService).markPaid("ORD-update");
        verify(paymentEventProducer).publishPaymentSucceeded("ORD-update", 150.00);
        verify(paymentLedgerRepository).save(any(PaymentLedgerEntity.class));
    }

    @Test
    void updatePayment_changeStatusToFailed_shouldSetCompletedAt() {
        PaymentRecord existing = PaymentRecord.builder()
                .orderId("ORD-fail")
                .status("PENDING")
                .paymentMethod("CARD")
                .amount(100.00)
                .build();

        when(paymentRepository.findByOrderId("ORD-fail"))
                .thenReturn(Optional.of(existing));

        when(paymentRepository.save(any(PaymentRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentRecord updated = paymentService.updatePayment("ORD-fail", "FAILED", null, null);

        assertEquals("FAILED", updated.getStatus());
        assertNotNull(updated.getCompletedAt());
        verify(orderService, never()).markPaid(anyString());
    }

    @Test
    void updatePayment_changeAmountAndMethod_shouldUpdate() {
        PaymentRecord existing = PaymentRecord.builder()
                .orderId("ORD-change")
                .status("PENDING")
                .paymentMethod("CARD")
                .amount(100.00)
                .build();

        when(paymentRepository.findByOrderId("ORD-change"))
                .thenReturn(Optional.of(existing));

        when(paymentRepository.save(any(PaymentRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentRecord updated = paymentService.updatePayment("ORD-change", null, "ALIPAY", 200.00);

        assertEquals("ALIPAY", updated.getPaymentMethod());
        assertEquals(200.00, updated.getAmount());
        assertEquals("PENDING", updated.getStatus()); // Status unchanged
    }

    @Test
    void updatePayment_notFound_shouldThrowException() {
        when(paymentRepository.findByOrderId("ORD-missing"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            paymentService.updatePayment("ORD-missing", "SUCCESS", null, null);
        });
    }
}
