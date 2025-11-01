package org.example.carpet.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String customerEmail,
        @NotBlank String sku,
        @Min(1) int quantity
) {}

