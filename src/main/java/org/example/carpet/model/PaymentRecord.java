package org.example.carpet.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRecord {

    @Id
    private String id;

    private String orderId;

    private double amount;

    private String paymentMethod; // CARD / MOBILE / ALIPAY / WECHATPAY

    private String status; // PENDING / SUCCESS / FAILED

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
