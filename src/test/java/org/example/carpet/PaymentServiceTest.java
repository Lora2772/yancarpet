package org.example.carpet.service;

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
 *  - submitPayment with CARD marks payment SUCCESS
 *  - submitPayment notifies orderService to mark order PAID
 *  - getPaymentStatus returns payment record by orderId
 *
 *  submitPayment("CARD")：
 * 创建一条 PaymentRecord 状态 "PENDING"
 * 立即把它标记为 "SUCCESS"
 * 调用 orderService.markPaid(orderId)，订单进入 "PAID" 状态
 *
 *  支付服务不只是写“我收钱了”
 * 支付服务还要通知订单服务更新状态
 * 也就是 Payment → Order 的业务耦合，后面就可以被替换成 Kafka 异步事件（PaymentSucceeded）。
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    OrderService orderService;

    @InjectMocks
    PaymentService paymentService;

    @Test
    void submitPayment_cardShouldMarkSuccessAndMarkOrderPaid() {
        // fake order state change
        when(orderService.markPaid("ORD-123"))
                .thenReturn(
                        OrderDocument.builder()
                                .orderId("ORD-123")
                                .status("PAID")
                                .build()
                );

        // When saving PaymentRecord, just return the argument (simulate Mongo save())
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

        // make sure we told OrderService to mark it paid
        verify(orderService).markPaid("ORD-123");
    }

    @Test
    void getPaymentStatus_shouldReturnRecordFromRepo() {
        PaymentRecord mock = PaymentRecord.builder()
                .orderId("ORD-xyz")
                .status("SUCCESS")
                .paymentMethod("CARD")
                .amount(100.00)
                .build();

        when(paymentRepository.findByOrderId("ORD-xyz"))
                .thenReturn(Optional.of(mock));

        PaymentRecord out = paymentService.getPaymentStatus("ORD-xyz");

        assertEquals("ORD-xyz", out.getOrderId());
        assertEquals("SUCCESS", out.getStatus());
        assertEquals("CARD", out.getPaymentMethod());
    }
}
