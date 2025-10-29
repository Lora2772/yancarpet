package org.example.carpet.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDocument {

    @Id
    private String id;

    // SKU：每个地毯唯一标识
    private String sku;

    // 商品名称，例如 "Persian Wool Carpet"
    private String name;

    // 分类，例如 "wool carpet", "carpet tiles"
    private String category;

    // 可选颜色（多个）
    private List<String> colors;

    // 使用场景，例如 "living room", "office", "hotel"
    private List<String> roomTypes;

    // 尺寸（固定选项 + custom）
    private List<String> sizeOptions;

    // 图片 URL
    private String imageUrl;

    // 简要描述
    private String description;

    // 是否需要销售联系（true 表示是定制产品）
    private boolean contactSalesRequired;

    // 如果是定制产品，销售联系人信息
    private SalesContactInfo salesContactInfo;
}
