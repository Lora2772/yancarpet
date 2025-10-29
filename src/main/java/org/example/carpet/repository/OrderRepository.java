package org.example.carpet.repository;

import org.example.carpet.model.OrderDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OrderRepository extends MongoRepository<OrderDocument, String> {

    // 我们将对外展示的是 orderId（业务ID），不是 Mongo 的 ObjectId
    Optional<OrderDocument> findByOrderId(String orderId);
}
