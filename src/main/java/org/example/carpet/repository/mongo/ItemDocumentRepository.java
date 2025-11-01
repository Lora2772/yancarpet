package org.example.carpet.repository.mongo;

import java.util.Optional;
import org.example.carpet.model.ItemDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Mongo repository for items.
 * 基于 MongoDB 的商品库存仓库接口。
 */
public interface ItemDocumentRepository
        extends MongoRepository<ItemDocument, String>, ItemDocumentRepositoryCustom {

    Optional<ItemDocument> findBySku(String sku);
}
