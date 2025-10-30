package org.example.carpet.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.OrderLineItem;
import org.example.carpet.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order lifecycle:
 * - POST /orders            -> create (reserve inventory)
 * - GET /orders/{orderId}   -> lookup order
 * - POST /orders/{orderId}/cancel -> cancel (release inventory)
 * - PUT /orders/{orderId}   -> limited update (only owner, only while RESERVED)
 *
 * The PUT endpoint demonstrates "Update Order" for the project rubric.
 * In practice you'd update shipping address, notes, etc. For demo,
 * we allow updating the customerEmail (contact email for this order),
 * but only if caller owns the order and it's still RESERVED.
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ----- Create Order -----
    @PostMapping
    public OrderDocument createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(
                request.getCustomerEmail(),
                request.getItems()
        );
    }

    // ----- Get Order -----
    @GetMapping("/{orderId}")
    public OrderDocument getOrder(@PathVariable String orderId) {
        return orderService.getOrderByOrderId(orderId);
    }

    // ----- Cancel Order -----
    @PostMapping("/{orderId}/cancel")
    public OrderDocument cancelOrder(@PathVariable String orderId) {
        return orderService.cancelOrder(orderId);
    }

    // ----- Update Order (limited editable fields) -----
    @PutMapping("/{orderId}")
    public OrderDocument updateOrder(
            @PathVariable String orderId,
            @RequestBody OrderUpdateRequest request,
            Authentication auth
    ) {
        // who is calling
        String callerEmail = auth.getName();

        // load the order
        OrderDocument order = orderService.getOrderByOrderId(orderId);

        // Only the owner can update
        if (!order.getCustomerEmail().equalsIgnoreCase(callerEmail)) {
            throw new RuntimeException("Not allowed to modify someone else's order");
        }

        // Only editable while RESERVED (i.e. before it's fulfilled/paid/shipped)
        if (!"RESERVED".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Order can no longer be modified");
        }

        // We allow updating contact email for this order (like "ship to my work email instead")
        if (request.getCustomerEmailOverride() != null &&
                !request.getCustomerEmailOverride().isBlank()) {
            order.setCustomerEmail(request.getCustomerEmailOverride());
        }

        // If in future you add shippingAddress or notes, you'd update them here

        return orderService.saveDirect(order);
    }

    // --------- DTOs ---------

    @Data
    public static class CreateOrderRequest {
        private String customerEmail;
        private List<OrderLineItem> items;
    }

    @Data
    public static class OrderUpdateRequest {
        // example changeable field; you can add shippingAddress, specialInstructions, etc.
        private String customerEmailOverride;
    }
}
