package org.example.carpet.repository.mongo;

import org.example.carpet.model.ItemDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ItemDocumentRepository
        extends MongoRepository<ItemDocument, String>, ItemDocumentRepositoryCustom {

    Optional<ItemDocument> findBySku(String sku);

    // 相关推荐：按 roomType / keywords 命中任一项，且排除本 sku
    @Query("""
      { $and: [
        { "sku": { $ne: ?2 } },
        { $or: [
            { "roomType": { $in: ?0 } },
            { "keywords": { $in: ?1 } }
        ] }
      ] }
    """)
    List<ItemDocument> findRelated(List<String> roomTypes, List<String> keywords,
                                   String excludeSku, Pageable pageable);

    // 注意：tryDeduct / tryRestock 的实现放在自定义 fragment 里（见下）
}
