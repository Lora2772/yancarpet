package org.example.carpet.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory inventory store.
 * Later this can be replaced by a MongoDB collection like:
 * { sku: "RUG-12345", availableQuantity: 18 }
 */
@Repository
public class InventoryRepository {

    // 模拟库存：key=sku, value=库存数量
    private final Map<String, Integer> inventory = new ConcurrentHashMap<>();

    public InventoryRepository() {
        // 你可以预置一些SKU库存，demo用
        inventory.put("RUG-12345", 18);
        inventory.put("RUG-RED-001", 12);
        inventory.put("HOTEL-WALL2WALL", 0); // 这个可能是 contact sales only
    }

    // 返回可售数量
    public int getAvailableQuantity(String sku) {
        return inventory.getOrDefault(sku, 0);
    }

    // 尝试锁库存（下单前reserve）
    // 返回true代表锁定成功
    public boolean reserve(String sku, int quantity) {
        int current = inventory.getOrDefault(sku, 0);
        if (current >= quantity) {
            inventory.put(sku, current - quantity);
            return true;
        }
        return false;
    }

    // 释放库存（订单取消 / 支付失败）
    public void release(String sku, int quantity) {
        int current = inventory.getOrDefault(sku, 0);
        inventory.put(sku, current + quantity);
    }
}
