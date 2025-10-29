package org.example.carpet.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.carpet.model.PaymentRecord;
import org.example.carpet.service.PaymentService;
import org.springframework.web.bind.annotation.*;

/**
 * 支付接口：
 *
 * - POST /payments/submit
 *      用户点击‘Pay Now’时调用
 *      我们会创建支付记录 (PENDING)，然后模拟支付成功 (CARD 直接 SUCCESS)
 *      如果支付成功 -> 把订单状态从 RESERVED 标为 PAID
 *
 * - GET /payments/status/{orderId}
 *      查询订单的支付状态（PENDING / SUCCESS / FAILED）
 *
 * 备注：
 *   将来可以扩展 "WECHAT_QR" / "ALIPAY_QR"：
 *   第一次返回 PENDING + 二维码URL，
 *   前端扫码后再回调我们把它标成 SUCCESS，最后标记订单为 PAID。
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/submit")
    public PaymentRecord submitPayment(@RequestBody SubmitPaymentRequest request) {
        return paymentService.submitPayment(
                request.getOrderId(),
                request.getPaymentMethod(),
                request.getAmount()
        );
    }

    @GetMapping("/status/{orderId}")
    public PaymentRecord getPaymentStatus(@PathVariable String orderId) {
        return paymentService.getPaymentStatus(orderId);
    }

    @Data
    public static class SubmitPaymentRequest {
        private String orderId;
        private String paymentMethod; // "CARD", "WECHAT_QR", "ALIPAY_QR"
        private double amount;        // total checkout amount
    }
}
