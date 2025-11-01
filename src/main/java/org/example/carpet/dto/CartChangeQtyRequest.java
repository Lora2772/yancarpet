package org.example.carpet.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;


@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CartChangeQtyRequest {
    private String sku;
    private Integer quantity;
}