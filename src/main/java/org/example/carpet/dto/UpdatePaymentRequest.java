package org.example.carpet.dto;

import lombok.Data;

@Data
public class UpdatePaymentRequest {
    private String status;         // PENDING, SUCCESS, FAILED
    private String paymentMethod;  // CARD, MOBILE, ALIPAY, WECHATPAY
    private Double amount;         // optional amount update
}
