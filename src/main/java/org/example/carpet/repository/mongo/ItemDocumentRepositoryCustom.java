package org.example.carpet.repository.mongo;

public interface ItemDocumentRepositoryCustom {

    /**
     * 尝试扣减库存：仅当 stockQuantity >= qty 时成功，成功返回 1，失败返回 0。
     */
    int tryDeduct(String sku, int qty);

    /**
     * 回补库存（只要文档存在就 +qty）。成功返回 1，未找到返回 0。
     */
    int tryRestock(String sku, int qty);
}
