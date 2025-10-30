package org.example.carpet.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLedgerRepository extends JpaRepository<PaymentLedgerEntity, Long> {
}
