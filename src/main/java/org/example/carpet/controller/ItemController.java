package org.example.carpet.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.carpet.model.ItemDocument;
import org.example.carpet.service.ItemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    // ---- 管理员：新增/更新商品（演示可暂时对外开放） ----
    @PostMapping
    public ItemDocument upsert(@RequestBody ItemDocument doc) {
        return itemService.upsertItem(doc);
    }

    // ---- 列表（简单返回全部；如果你有分页，可换成分页返回） ----
    @GetMapping
    public List<ItemDocument> list() {
        // 你也可以在 service 里做分页/排序，这里简化为 findAll
        return itemService.search(null, null, null, null);
    }

    // ---- 详情 ----
    @GetMapping("/{sku}")
    public ItemDocument detail(@PathVariable String sku) {
        return itemService.getBySku(sku);
    }

    // ---- 搜索/过滤 ----
    // 例：/items/search?q=wool&category=rug&color=blue&roomType=living room
    @GetMapping("/search")
    public List<ItemDocument> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String color,
            @RequestParam(required = false, name = "roomType") String roomType
    ) {
        return itemService.search(q, category, color, roomType);
    }

    // ---- 推荐（基于某个 SKU 的相似项）----
    // 例：/items/{sku}/recommendations?limit=8
    @GetMapping("/{sku}/recommendations")
    public List<ItemDocument> recommendBySku(
            @PathVariable String sku,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return itemService.recommendBySku(sku, limit);
    }

    // ---- 推荐（显式传入标签）----
    // 例：/items/recommendations?roomType=living%20room&keywords=wool,handmade&limit=8
    @GetMapping("/recommendations")
    public List<ItemDocument> recommendByTags(
            @RequestParam(required = false, name = "roomType") String roomType,
            @RequestParam(required = false) String keywords,
            @RequestParam(defaultValue = "8") int limit
    ) {
        RecommendTags req = new RecommendTags();
        req.setRoomType(roomType);
        req.setKeywords(keywords);
        req.setLimit(limit);
        return itemService.recommendByTags(req.getRoomType(), req.getKeywords(), req.getLimit());
    }

    @Data
    public static class RecommendTags {
        private String roomType;   // 逗号分隔或单值均可，service 内会拆分
        private String keywords;   // 逗号分隔或单值均可
        private int    limit = 8;
    }
}
