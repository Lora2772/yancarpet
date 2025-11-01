package org.example.carpet.repository.mongo;

/**
 * Custom extension for atomic stock operations.
 */
public interface ItemDocumentRepositoryCustom {
    /**
     * 原子扣减库存（仅当可用数量 >= qty 时才更新）
     * @return 1 表示成功，0 表示库存不足或不存在
     */
    int tryDeduct(String sku, int qty);

    /**
     * 原子回补库存（在预留失败时回滚）
     */
    int tryRestock(String sku, int qty);
}
