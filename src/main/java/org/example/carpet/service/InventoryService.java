package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.repository.InventoryRepository;
import org.springframework.stereotype.Service;

/**
 * Handles stock availability and shipping promise.
 * For now, inventory is in-memory (InventoryRepository).
 * Later this can move to its own microservice + Kafka.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    // 查询库存 + 返回承诺运输时间
    public InventoryStatus checkInventory(String sku) {
        int qty = inventoryRepository.getAvailableQuantity(sku);
        return InventoryStatus.builder()
                .sku(sku)
                .availableQuantity(qty)
                .estimatedDeliveryBusinessDays(15)
                .estimatedDeliveryText("approximately 15 business days")
                .notes("Ships via sea freight from our overseas warehouse.")
                .build();
    }

    // 锁库存（用于创建订单）
    public boolean reserve(String sku, int quantity) {
        return inventoryRepository.reserve(sku, quantity);
    }

    // 释放库存（订单取消 / 支付失败）
    public void release(String sku, int quantity) {
        inventoryRepository.release(sku, quantity);
    }

    // 内部使用的结构体（不放 dto 包是因为这是 service <-> controller 的中间数据）
    @lombok.Data
    @lombok.Builder
    public static class InventoryStatus {
        private String sku;
        private int availableQuantity;
        private int estimatedDeliveryBusinessDays;
        private String estimatedDeliveryText;
        private String notes;
    }
}
