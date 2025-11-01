package org.example.carpet.repository.mongo;

import org.example.carpet.model.ItemDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * Implementation of atomic stock operations using MongoTemplate.
 */
@Repository
public class ItemDocumentRepositoryImpl implements ItemDocumentRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public ItemDocumentRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public int tryDeduct(String sku, int qty) {
        Query q = Query.query(
                Criteria.where("sku").is(sku)
                        .and("stockQuantity").gte(qty)
        );
        Update u = new Update().inc("stockQuantity", -qty);
        ItemDocument before = mongoTemplate.findAndModify(q, u, ItemDocument.class);
        if (before == null) {
            return 0; // 库存不足或不存在
        }

        int newQty = (before.getStockQuantity() == null ? 0 : before.getStockQuantity()) - qty;
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("sku").is(sku)),
                new Update().set("stockAvailable", newQty > 0),
                ItemDocument.class
        );
        return 1;
    }

    @Override
    public int tryRestock(String sku, int qty) {
        Query q = Query.query(Criteria.where("sku").is(sku));
        Update u = new Update().inc("stockQuantity", qty);
        ItemDocument before = mongoTemplate.findAndModify(q, u, ItemDocument.class);
        if (before == null) {
            return 0;
        }

        int newQty = (before.getStockQuantity() == null ? 0 : before.getStockQuantity()) + qty;
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("sku").is(sku)),
                new Update().set("stockAvailable", newQty > 0),
                ItemDocument.class
        );
        return 1;
    }
}
