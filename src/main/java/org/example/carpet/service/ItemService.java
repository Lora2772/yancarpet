package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.model.ItemDocument;
import org.example.carpet.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handles product catalog logic:
 * - add/update product
 * - get single product by SKU
 * - search / filter products for front-end
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    // 创建或更新商品 (管理员用)
    public ItemDocument upsertItem(ItemDocument doc) {
        // If SKU already exists, overwrite the doc with same SKU.
        Optional<ItemDocument> existing = itemRepository.findBySku(doc.getSku());
        existing.ifPresent(e -> doc.setId(e.getId())); // keep same _id if updating
        return itemRepository.save(doc);
    }

    // 商品详情
    public ItemDocument getBySku(String sku) {
        return itemRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Item not found for sku: " + sku));
    }

    // 搜索 + 过滤
    // 支持前端的:
    // q          (任意关键词、sku、名称)
    // category   (比如 wool carpet / carpet tiles / rugs / hotel carpet / factory carpet...)
    // color      (red / blue / gray / ivory...)
    // roomType   (living room / office / hotel / bathroom / bedroom...)
    public List<ItemDocument> search(String q, String category, String color, String roomType) {

        List<ItemDocument> all = itemRepository.findAll();

        return all.stream()
                .filter(item -> match(item, q, category, color, roomType))
                .collect(Collectors.toList());
    }

    private boolean match(ItemDocument item,
                          String q,
                          String category,
                          String color,
                          String roomType) {

        String skuLc = safe(item.getSku());
        String nameLc = safe(item.getName());
        String descLc = safe(item.getDescription());
        String colorJoined = safeList(item.getColors());
        String categoriesJoined = safe(item.getCategory()); // single category main tag
        String roomTypesJoined = safeList(item.getRoomTypes());

        boolean okQ = (q == null)
                || skuLc.contains(q.toLowerCase(Locale.ROOT))
                || nameLc.contains(q.toLowerCase(Locale.ROOT))
                || descLc.contains(q.toLowerCase(Locale.ROOT))
                || categoriesJoined.contains(q.toLowerCase(Locale.ROOT))
                || roomTypesJoined.contains(q.toLowerCase(Locale.ROOT));

        boolean okCategory = (category == null)
                || categoriesJoined.contains(category.toLowerCase(Locale.ROOT));

        boolean okColor = (color == null)
                || colorJoined.contains(color.toLowerCase(Locale.ROOT));

        boolean okRoomType = (roomType == null)
                || roomTypesJoined.contains(roomType.toLowerCase(Locale.ROOT));

        return okQ && okCategory && okColor && okRoomType;
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private String safeList(List<String> list) {
        if (list == null) return "";
        return list.stream()
                .filter(v -> v != null)
                .map(v -> v.toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }
}
