package org.example.carpet.repository;

import org.example.carpet.model.PaymentRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PaymentRepository extends MongoRepository<PaymentRecord, String> {

    Optional<PaymentRecord> findByOrderId(String orderId);
}
