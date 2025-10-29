package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.service.InventoryService;
import org.springframework.web.bind.annotation.*;

/**
 * 库存和交付时效相关接口
 *
 * - GET /inventory/check?sku=RUG-12345
 *      返回可售数量 + 运输承诺时间（approximately 15 business days）
 *
 * 说明：
 * 只读查询接口，不改变库存
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/check")
    public InventoryService.InventoryStatus check(@RequestParam String sku) {
        return inventoryService.checkInventory(sku);
    }

    @PostMapping("/reserve")
    public boolean reserve(@RequestParam String sku, @RequestParam int quantity) {
        return inventoryService.reserve(sku, quantity);
    }

    @PostMapping("/release")
    public void release(@RequestParam String sku, @RequestParam int quantity) {
        inventoryService.release(sku, quantity);
    }
}
