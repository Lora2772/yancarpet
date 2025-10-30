package org.example.carpet.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "items")
public class ItemDocument {
    @Id
    private String id;

    private String sku;
    private String name;

    // 可选分类
    private String category;

    // 颜色 / 材质（注意现在是单值 String）
    private String color;
    private String material;

    // 房间类型（数组）。兼容老的 roomTypes 入参
    @JsonAlias({"roomTypes"})
    private List<String> roomType;

    private List<String> sizeOptions;

    private String imageUrl;
    private String description;

    // ✅ 价格与单位（关键）
    private BigDecimal unitPrice;   // 如 140
    private String unit;            // 如 "usd/sqm"

    private Boolean stockAvailable;
    private List<String> keywords;

    private Boolean contactSalesRequired;
    private String salesContactInfo;
}
