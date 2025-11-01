package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.example.carpet.model.ItemDocument;
import org.example.carpet.repository.mongo.ItemDocumentRepository;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.data.cassandra.core.CassandraOperations;


import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ItemService - 产品目录 / 搜索 / 推荐 + Cassandra购物车整合
 *
 * Mongo:
 *  - add/update 商品、详情、搜索、推荐
 *
 * Cassandra (cart_items_by_user):
 *  - 购物车 upsert / list / remove / clear （行级 TTL）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {

    // ===== Mongo (产品目录/推荐) =====
    private final ItemDocumentRepository itemRepository;
    private final MongoTemplate mongoTemplate;

    // ===== Cassandra (购物车) =====
    private final CassandraTemplate cassandraTemplate;


    // ------------------------------------------------------------
    // 商品增改 / 详情 / 搜索
    // ------------------------------------------------------------

    /** 创建或更新商品（管理员） */
    public ItemDocument upsertItem(ItemDocument doc) {
        itemRepository.findBySku(doc.getSku())
                .ifPresent(e -> doc.setId(e.getId())); // 保持 _id 不变以覆盖
        return itemRepository.save(doc);
    }

    /** 商品详情 */
    public ItemDocument getBySku(String sku) {
        return itemRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Item not found for sku: " + sku));
    }

    /** 简单搜索 + 过滤（本地内存过滤；量大时建议改 Mongo 查询条件） */
    public List<ItemDocument> search(String q, String category, String color, String roomType) {
        final List<String> qsCategories = splitCsv(category);
        final List<String> qsColors     = splitCsv(color);
        final List<String> qsRooms      = splitCsv(roomType);

        List<ItemDocument> all = itemRepository.findAll();

        return all.stream().filter(item -> {
            final String skuLc   = lc(item.getSku());
            final String nameLc  = lc(item.getName());
            final String descLc  = lc(item.getDescription());
            final String catJoin = joinAny(item.getCategory());
            final String colJoin = joinAny(item.getColor());
            final String rtJoin  = joinAny(item.getRoomType());

            boolean okQ = isEmpty(q) ||
                    skuLc.contains(lc(q)) ||
                    nameLc.contains(lc(q)) ||
                    descLc.contains(lc(q)) ||
                    catJoin.contains(lc(q)) ||
                    rtJoin.contains(lc(q));

            boolean okCategory = qsCategories.isEmpty() || containsAny(catJoin, qsCategories);
            boolean okColor    = qsColors.isEmpty()     || containsAny(colJoin, qsColors);
            boolean okRoom     = qsRooms.isEmpty()      || containsAny(rtJoin, qsRooms);

            return okQ && okCategory && okColor && okRoom;
        }).collect(Collectors.toList());
    }

    // ------------------------------------------------------------
    // 推荐（by sku / by tags）
    // ------------------------------------------------------------

    /** 推荐主入口（供 /items/recommend 调用） */
    public List<ItemDocument> recommend(String sku, int limit) {
        return recommendBySku(sku, limit);
    }

    /** 根据 sku 推荐（找出该商品的 roomType/keywords -> 再做标签召回） */
    public List<ItemDocument> recommendBySku(String sku, int limit) {
        ItemDocument base = itemRepository.findBySku(sku).orElse(null);
        if (base == null) return List.of();

        List<String> rooms = getStringList(base.getRoomType());
        List<String> kws   = getStringList(base.getKeywords());

        return recommendByTags(String.join(",", rooms), String.join(",", kws), limit)
                .stream()
                .filter(it -> !Objects.equals(it.getSku(), sku))   // 排除自己
                .collect(Collectors.toList());
    }

    /**
     * 按标签推荐（支持 roomType / keywords；任一匹配即入选）
     * 无标签时随机兜底
     */
    public List<ItemDocument> recommendByTags(String roomType, String keywords, int limit) {
        Set<String> tokenSet = new LinkedHashSet<>();
        if (roomType != null) {
            Arrays.stream(roomType.split("[,;\\s]+"))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .forEach(tokenSet::add);
        }
        if (keywords != null) {
            Arrays.stream(keywords.split("[,;\\s]+"))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .forEach(tokenSet::add);
        }

        // 无标签 → 随机推荐
        if (tokenSet.isEmpty()) {
            List<ItemDocument> all = itemRepository.findAll();
            Collections.shuffle(all);
            return all.stream().limit(limit).collect(Collectors.toList());
        }

        // 构造 Mongo OR 查询
        List<Criteria> ors = new ArrayList<>();
        for (String token : tokenSet) {
            String regex = ".*" + escapeRegex(token) + ".*";
            ors.add(new Criteria().orOperator(
                    Criteria.where("roomType").regex(regex, "i"),
                    Criteria.where("keywords").regex(regex, "i"),
                    Criteria.where("name").regex(regex, "i"),
                    Criteria.where("material").regex(regex, "i"),
                    Criteria.where("color").regex(regex, "i")
            ));
        }
        Query query = new Query(new Criteria().orOperator(ors.toArray(new Criteria[0])))
                .limit(Math.max(1, limit));

        List<ItemDocument> results = mongoTemplate.find(query, ItemDocument.class);
        if (results.isEmpty()) {
            // 兜底（防止页面空白）
            List<ItemDocument> all = itemRepository.findAll();
            Collections.shuffle(all);
            return all.stream().limit(limit).collect(Collectors.toList());
        }
        return results;
    }

    // ------------------------------------------------------------
    // Cassandra：购物车（行级 TTL）
    // 表：cart_items_by_user(user_email text, sku text, qty int, price decimal, updated_at_ts bigint, PRIMARY KEY(user_email, sku))
    // ------------------------------------------------------------

    /** upsert 购物车条目；默认 TTL 30 天，可传自定义 TTL */
    public void cartUpsertCassandra(String userEmail, String sku, int qty, BigDecimal price, Duration ttl) {
        long now = System.currentTimeMillis();
        int ttlSec = (int) Math.max(1, (ttl != null ? ttl.getSeconds() : Duration.ofDays(30).getSeconds()));
        cassandraTemplate.getCqlOperations().execute(
                "INSERT INTO cart_items_by_user (user_email, sku, qty, price, updated_at_ts) " +
                        "VALUES (?, ?, ?, ?, ?) USING TTL ?",
                userEmail, sku, qty, price, now, ttlSec
        );
    }

    /** 查询购物车 */
    public List<CartItemView> cartListCassandra(String userEmail) {
        var rows = cassandraTemplate.getCqlOperations().queryForRows(
                "SELECT user_email, sku, qty, price, updated_at_ts " +
                        "FROM cart_items_by_user WHERE user_email = ?",
                userEmail
        );
        List<CartItemView> out = new ArrayList<>();
        rows.forEach(row -> out.add(new CartItemView(
                row.getString("user_email"),
                row.getString("sku"),
                row.getInt("qty"),
                row.getBigDecimal("price"),
                row.getLong("updated_at_ts")
        )));
        return out;
    }

    /** 删除某个 SKU */
    public void cartRemoveCassandra(String userEmail, String sku) {
        cassandraTemplate.getCqlOperations().execute(
                "DELETE FROM cart_items_by_user WHERE user_email = ? AND sku = ?",
                userEmail, sku
        );
    }

    /** 清空购物车（也可仅依赖 TTL 自然过期） */
    public void cartClearCassandra(String userEmail) {
        for (CartItemView it : cartListCassandra(userEmail)) {
            cartRemoveCassandra(userEmail, it.getSku());
        }
    }

    // 购物车视图对象（给 controller 返回）
    @Value
    public static class CartItemView {
        String userEmail;
        String sku;
        int qty;
        BigDecimal price;
        long updatedAtTs;
    }

    // ------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------
    private static String lc(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
    private static boolean isEmpty(String s) { return s == null || s.isBlank(); }

    /** 将 String 或 List<String> 统一拍平成小写空格拼接文本，便于 contains */
    private static String joinAny(Object v) {
        if (v == null) return "";
        if (v instanceof String) return lc((String) v);
        if (v instanceof Collection<?>) {
            return ((Collection<?>) v).stream()
                    .filter(Objects::nonNull)
                    .map(o -> lc(String.valueOf(o)))
                    .collect(Collectors.joining(" "));
        }
        return lc(String.valueOf(v));
    }

    /** 解析逗号分隔列表为小写 token 列表 */
    private static List<String> splitCsv(String s) {
        if (isEmpty(s)) return Collections.emptyList();
        return Arrays.stream(s.split(","))
                .map(String::trim).filter(t -> !t.isEmpty())
                .map(t -> t.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    /** haystack 是 joinAny 的结果；needles 是已小写的词列表，只要有一个出现就匹配 */
    private static boolean containsAny(String haystack, List<String> needles) {
        if (haystack.isEmpty() || needles.isEmpty()) return true;
        for (String n : needles) if (haystack.contains(n)) return true;
        return false;
    }

    /** 将字段（String / List / 其它）安全转为字符串列表（不使用 Java21 的模式匹配） */
    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Object field) {
        List<String> out = new ArrayList<>();
        if (field == null) return out;
        if (field instanceof Collection<?>) {
            for (Object o : (Collection<?>) field) {
                if (o != null) out.add(String.valueOf(o));
            }
        } else {
            out.add(String.valueOf(field));
        }
        return out;
    }

    /** 简单转义正则特殊字符，避免用户关键词破坏 regex */
    private static String escapeRegex(String s) {
        return s.replaceAll("([\\\\.^$|?*+()\\[\\]{}])", "\\\\$1");
    }
}
