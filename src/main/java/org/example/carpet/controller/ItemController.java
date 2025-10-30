package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.model.ItemDocument;
import org.example.carpet.service.ItemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品目录 / 搜索 / 定制页信息
 *
 * - POST /items            管理员录入或更新一个SKU
 * - GET  /items            列表（可带同样的 query 参数，等同于 /items/search）
 * - GET  /items/{sku}      用户查看商品详情页
 * - GET  /items/search     用户在前端搜索框+filter检索
 */
@CrossOrigin(
        origins = { "http://localhost:5173", "http://127.0.0.1:5173" },
        allowCredentials = "true"   // 如果前端带了 cookies/凭证
)
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

    // ✅ 新增：列表（与 /items/search 相同的查询参数，方便前端直接用 /items）
    // 例如：/items?q=wool&category=hotel%20carpet&color=red&roomType=living%20room
    @GetMapping
    public List<ItemDocument> listItems(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String roomType
    ) {
        return itemService.search(q, category, color, roomType);
    }

    // 商品详情
    @GetMapping("/{sku}")
    public ItemDocument getItem(@PathVariable String sku) {
        return itemService.getBySku(sku);
    }

    // 搜索 + filter（保留老路径，兼容前端两种写法）
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
