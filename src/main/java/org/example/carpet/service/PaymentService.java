package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.model.PaymentRecord;
import org.example.carpet.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

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

    // 用户提交支付
    public PaymentRecord submitPayment(String orderId, String paymentMethod, double amount) {

        // 1. 先在DB里创建一笔支付记录，状态 PENDING
        PaymentRecord record = PaymentRecord.builder()
                .id(null)
                .orderId(orderId)
                .amount(amount)
                .paymentMethod(paymentMethod) // "CARD", "WECHAT_QR", etc.
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .completedAt(null)
                .build();

        record = paymentRepository.save(record);

        // 2. 模拟立即支付成功 (对 CARD 来说我们可以假装永远成功)
        if ("CARD".equalsIgnoreCase(paymentMethod)) {
            record.setStatus("SUCCESS");
            record.setCompletedAt(LocalDateTime.now());
            record = paymentRepository.save(record);

            // 3. 更新订单状态为 PAID
            orderService.markPaid(orderId);
        }

        // 未来如果是 WECHAT_QR / ALIPAY_QR:
        // - 返回 status = PENDING + qrCodeUrl
        // - 等前端轮询/回调后再标记 SUCCESS + markPaid(orderId)

        return record;
    }

    // 查询支付状态（checkout 页面可以轮询）
    public PaymentRecord getPaymentStatus(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("No payment found for order " + orderId));
    }
}
