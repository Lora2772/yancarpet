package org.example.carpet.repository.jpa;

import org.example.carpet.ledger.PaymentLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentLedgerRepository extends JpaRepository<PaymentLedgerEntity, Long> {

    // 按订单查询
    List<PaymentLedgerEntity> findAllByOrderId(String orderId);

    // 按状态查询
    List<PaymentLedgerEntity> findAllByStatus(String status);

    // 按支付方式查询（例如 CARD / PAYPAL）
    List<PaymentLedgerEntity> findAllByPaymentMethod(String paymentMethod);

    // 全量时间区间（字段名必须与实体一致：recordedAt）
    List<PaymentLedgerEntity> findAllByRecordedAtBetween(LocalDateTime start, LocalDateTime end);

    // 组合条件：某个订单在时间区间内的记录
    List<PaymentLedgerEntity> findAllByOrderIdAndRecordedAtBetween(
            String orderId, LocalDateTime start, LocalDateTime end);
}
