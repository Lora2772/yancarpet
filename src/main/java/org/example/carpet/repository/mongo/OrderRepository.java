package org.example.carpet.repository.mongo;

import org.example.carpet.model.OrderDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OrderRepository extends MongoRepository<OrderDocument, String> {

    Optional<OrderDocument> findByOrderId(String orderId);

    // 新增：按用户邮箱分页查历史订单（用 Pageable 里携带的 Sort 做降序）
    Page<OrderDocument> findByCustomerEmail(String customerEmail, Pageable pageable);
}
