package org.example.carpet.repository;

import org.example.carpet.model.ItemDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends MongoRepository<ItemDocument, String> {

    // 根据 SKU 精确查商品
    Optional<ItemDocument> findBySku(String sku);

    // 根据分类关键字 (e.g. "wool carpet", "carpet tiles", "hotel carpet"...)
    List<ItemDocument> findByCategoryIgnoreCase(String category);

    // 根据颜色是否包含
    List<ItemDocument> findByColorsIgnoreCase(String color);

    // 根据 roomTypes 是否包含 (e.g. "hotel", "office", "living room")
    List<ItemDocument> findByRoomTypesIgnoreCase(String roomType);

    // 简单全文搜索（名字 or 描述 包含关键字）——这只是示例，后续我们也会在 service 里做手工 filter
    List<ItemDocument> findByNameContainingIgnoreCase(String keyword);

    List<ItemDocument> findByDescriptionContainingIgnoreCase(String keyword);
}
