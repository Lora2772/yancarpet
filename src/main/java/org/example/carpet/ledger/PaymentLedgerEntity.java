package org.example.carpet.ledger;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_ledger")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private Double amountUsd;
    private String paymentMethod;
    private String status; // "SUCCESS" or "REFUND_SUCCESS"
    private LocalDateTime recordedAt;
}
