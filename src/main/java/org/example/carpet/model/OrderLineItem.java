package org.example.carpet.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderLineItem {
    private String sku;
    private String name;
    private int quantity;
    private double price;
    private String sizeOption;
}
