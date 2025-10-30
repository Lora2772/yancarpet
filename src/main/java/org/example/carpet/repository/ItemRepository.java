package org.example.carpet.repository;

import org.example.carpet.model.ItemDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ItemRepository extends MongoRepository<ItemDocument, String> {
    Optional<ItemDocument> findBySku(String sku);
    // ❌ 删掉所有像 findByColorsIgnoreCase / findByRoomTypes... 这些老签名
}
