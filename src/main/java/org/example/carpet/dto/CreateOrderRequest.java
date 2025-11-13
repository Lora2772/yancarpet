package org.example.carpet.dto;

import lombok.Data;
import org.example.carpet.model.OrderLineItem;

import java.util.List;

@Data
public class CreateOrderRequest {
    private String customerEmail;
    private List<OrderLineItem> items;
}
