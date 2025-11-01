package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.model.ItemDocument;
import org.example.carpet.repository.mongo.ItemDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles product catalog logic:
 * - add/update product
 * - get single product by SKU
 * - search / filter products for front-end
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemDocumentRepository itemRepository;

    // 创建或更新商品 (管理员用)
    public ItemDocument upsertItem(ItemDocument doc) {
        itemRepository.findBySku(doc.getSku()).ifPresent(e -> doc.setId(e.getId())); // keep same _id if updating
        return itemRepository.save(doc);
    }

    // 商品详情
    public ItemDocument getBySku(String sku) {
        return itemRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Item not found for sku: " + sku));
    }

    /**
     * 搜索 + 过滤
     * 支持:
     *  q          任意关键词/sku/名称/描述/分类/房间
     *  category   单值或逗号分隔
     *  color      单值或逗号分隔（模型里是 String，也能匹配）
     *  roomType   单值或逗号分隔（模型里是 List<String>）
     */
    public List<ItemDocument> search(String q, String category, String color, String roomType) {
        final List<String> qsCategories = splitCsv(category);
        final List<String> qsColors    = splitCsv(color);
        final List<String> qsRooms     = splitCsv(roomType);

        List<ItemDocument> all = itemRepository.findAll();

        return all.stream().filter(item -> {
            // 归一化文档里的字段：支持 String 或 List<String>
            final String skuLc   = lc(item.getSku());
            final String nameLc  = lc(item.getName());
            final String descLc  = lc(item.getDescription());
            final String catJoin = joinAny(item.getCategory());          // String
            final String colJoin = joinAny(item.getColor());             // String
            final String rtJoin  = joinAny(item.getRoomType());          // List<String>

            // 关键字匹配
            boolean okQ = isEmpty(q) ||
                    skuLc.contains(lc(q)) ||
                    nameLc.contains(lc(q)) ||
                    descLc.contains(lc(q)) ||
                    catJoin.contains(lc(q)) ||
                    rtJoin.contains(lc(q));

            // 过滤项匹配（支持多选）
            boolean okCategory = qsCategories.isEmpty() || containsAny(catJoin, qsCategories);
            boolean okColor    = qsColors.isEmpty()    || containsAny(colJoin, qsColors);
            boolean okRoom     = qsRooms.isEmpty()     || containsAny(rtJoin, qsRooms);

            return okQ && okCategory && okColor && okRoom;
        }).collect(Collectors.toList());
    }

    // ===== helpers =====

    private static String lc(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }

    /** 支持 String 或 List<String>，统一拼成小写的空格分隔字符串，便于 contains */
    private static String joinAny(Object v) {
        if (v == null) return "";
        if (v instanceof String) {
            return lc((String) v);
        }
        if (v instanceof Collection<?>) {
            return ((Collection<?>) v).stream()
                    .filter(Objects::nonNull)
                    .map(o -> lc(String.valueOf(o)))
                    .collect(Collectors.joining(" "));
        }
        return lc(String.valueOf(v));
    }

    /** 解析逗号分隔的查询参数，转成小写列表（空/全空白返回空列表） */
    private static List<String> splitCsv(String s) {
        if (isEmpty(s)) return Collections.emptyList();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(t -> t.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    /** haystack 是 joinAny 的结果；needles 是已小写的词列表，只要有一个出现就匹配 */
    private static boolean containsAny(String haystack, List<String> needles) {
        if (haystack.isEmpty() || needles.isEmpty()) return true;
        for (String n : needles) {
            if (haystack.contains(n)) return true;
        }
        return false;
    }
}
