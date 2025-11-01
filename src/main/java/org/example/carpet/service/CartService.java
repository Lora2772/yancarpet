package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.cassandra.entities.CartItem;
import org.example.carpet.cassandra.keys.CartItemKey;
import org.example.carpet.cassandra.repos.CartItemRepo;
import org.example.carpet.dto.CartChangeQtyRequest;
import org.example.carpet.dto.CartItemView;
import org.example.carpet.dto.CartUpsertRequest;
import org.example.carpet.model.ItemDocument;
import org.example.carpet.repository.mongo.ItemDocumentRepository;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepo cartItemRepo;                 // Cassandra repo
    private final ItemDocumentRepository itemRepo;           // Mongo for product info
    private final CassandraTemplate cassandraTemplate;


    /** 列出购物车条目（Cassandra -> 补全 Mongo 商品信息） */
    public List<CartItemView> list(String userEmail) {
        return cartItemRepo.findByUserEmail(userEmail).stream()
                .map(ci -> {
                    String sku = ci.getKey().getSku();
                    Optional<ItemDocument> doc = itemRepo.findBySku(sku);
                    String name = doc.map(ItemDocument::getName).orElse(sku);
                    String imageUrl = doc.map(ItemDocument::getImageUrl).orElse(null);
                    List<String> roomType = doc.map(ItemDocument::getRoomType).orElse(List.of());
                    List<String> keywords = Optional.ofNullable(doc.map(ItemDocument::getKeywords).orElse(null))
                            .orElse(List.of());
                    return CartItemView.builder()
                            .sku(sku)
                            .name(name)
                            .imageUrl(imageUrl)
                            .roomType(roomType)
                            .keywords(keywords)
                            .quantity(ci.getQty())
                            .price(ci.getPrice())
                            .build();
                })
                .toList();
    }

    /** 控制器调用：添加/更新购物车（数量为 null 则 +1；否则设为给定数量） */
    public void upsert(String userEmail, CartUpsertRequest req) {
        // price 为空时，从商品库兜底
        BigDecimal price = req.getPrice();
        if (price == null) {
            price = itemRepo.findBySku(req.getSku())
                    .map(ItemDocument::getUnitPrice)
                    .orElse(BigDecimal.ZERO);
        }
        upsert(userEmail, req.getSku(), req.getQuantity(), price);
    }

    /** 可重用的底层 upsert */
    public void upsert(String userEmail, String sku, Integer qty, BigDecimal price) {
        CartItemKey key = new CartItemKey(userEmail, sku);
        CartItem current = cartItemRepo.findById(key)
                .orElse(new CartItem(key, 0, price != null ? price : BigDecimal.ZERO, Instant.now().toEpochMilli()));

        int newQty = (qty == null ? current.getQty() + 1 : qty);
        if (newQty <= 0) {
            cartItemRepo.deleteById(key);
            return;
        }

        current.setQty(newQty);
        if (price != null) current.setPrice(price);
        current.setUpdatedAtTs(Instant.now().toEpochMilli());

        // 首选：使用 CQL 写入并加 TTL（30 天）。若 cassandra 为 null，则退回 save()
        if (cassandraTemplate != null) {
            int ttlDays = 30;
            cassandraTemplate.getCqlOperations().execute(
                    "INSERT INTO cart_items_by_user (user_email, sku, qty, price, updated_at_ts) " +
                            "VALUES (?, ?, ?, ?, ?) USING TTL ?",
                    userEmail, sku, current.getQty(), current.getPrice(), current.getUpdatedAtTs(), ttlDays * 24 * 3600
            );
        } else {
            cartItemRepo.save(current);
        }
    }

    /** 控制器调用：修改数量（绝对值；<=0 删条目） */
    public void changeQty(String userEmail, CartChangeQtyRequest req) {
        CartItemKey key = new CartItemKey(userEmail, req.getSku());
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            cartItemRepo.deleteById(key);
            return;
        }
        CartItem current = cartItemRepo.findById(key)
                .orElseGet(() -> new CartItem(key, 0, // 价格兜底
                        itemRepo.findBySku(req.getSku())
                                .map(ItemDocument::getUnitPrice)
                                .orElse(BigDecimal.ZERO),
                        Instant.now().toEpochMilli()));

        current.setQty(req.getQuantity());
        current.setUpdatedAtTs(Instant.now().toEpochMilli());

        if (cassandraTemplate != null) {
            int ttlDays = 30;
            cassandraTemplate.getCqlOperations().execute(
                    "INSERT INTO cart_items_by_user (user_email, sku, qty, price, updated_at_ts) " +
                            "VALUES (?, ?, ?, ?, ?) USING TTL ?",
                    userEmail, req.getSku(), current.getQty(), current.getPrice(), current.getUpdatedAtTs(), ttlDays * 24 * 3600
            );
        } else {
            cartItemRepo.save(current);
        }
    }

    /** 删除一个 SKU */
    public void remove(String userEmail, String sku) {
        cartItemRepo.deleteById(new CartItemKey(userEmail, sku));
    }

    /** 清空购物车 */
    public void clear(String userEmail) {
        cartItemRepo.findByUserEmail(userEmail)
                .forEach(ci -> cartItemRepo.deleteById(ci.getKey()));
    }
}
