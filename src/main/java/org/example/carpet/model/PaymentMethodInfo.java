package org.example.carpet.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodInfo {
    // e.g. "CARD", "WECHAT_QR", "ALIPAY_QR"
    private String type;

    // human-readable masked detail:
    // e.g. "VISA **** 1234", "WeChatPay acct ...", "Alipay acct ..."
    private String maskedDetail;
}
