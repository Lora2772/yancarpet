package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.model.ItemDocument;
import org.example.carpet.service.ItemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品目录 / 搜索 / 定制页信息
 *
 * - POST /items
 *      管理员录入或更新一个SKU(标准商品 or 定制型产品)
 *
 * - GET /items/{sku}
 *      用户查看商品详情页
 *
 * - GET /items/search
 *      用户在前端搜索框+filter检索:
 *      支持 query params: q, category, color, roomType
 */
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    // 管理员添加/更新一个商品
    @PostMapping
    public ItemDocument upsertItem(@RequestBody ItemDocument request) {
        return itemService.upsertItem(request);
    }

    // 商品详情 (标准SKU or 定制SKU)
    @GetMapping("/{sku}")
    public ItemDocument getItem(@PathVariable String sku) {
        return itemService.getBySku(sku);
    }

    // 搜索 + filter
    // /items/search?q=wool%20carpet&category=hotel%20carpet&color=red&roomType=living%20room
    @GetMapping("/search")
    public List<ItemDocument> searchItems(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String roomType
    ) {
        return itemService.search(q, category, color, roomType);
    }
}
