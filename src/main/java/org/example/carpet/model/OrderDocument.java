package org.example.carpet.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDocument {

    @Id
    private String id;

    private String orderId;

    private List<OrderLineItem> items;

    private String customerEmail;

    private Address shippingAddress;

    private double totalAmount;

    private String status; // CREATED / RESERVED / PAID / CANCELLED

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
