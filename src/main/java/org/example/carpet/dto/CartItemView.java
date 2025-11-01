package org.example.carpet.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;


@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CartItemView {
    private String sku;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
    private List<String> roomType;
    private List<String> keywords;
}
