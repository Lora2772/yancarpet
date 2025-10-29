package org.example.carpet.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.OrderLineItem;
import org.example.carpet.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单相关接口：
 *
 * - POST /orders
 *      创建一个订单（会尝试锁库存）
 *
 * - GET /orders/{orderId}
 *      查询订单详情（状态: RESERVED / PAID / CANCELLED）
 *
 * - POST /orders/{orderId}/cancel
 *      取消订单（会释放库存）
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 创建订单
    @PostMapping
    public OrderDocument createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(
                request.getCustomerEmail(),
                request.getItems()
        );
    }

    // 查订单
    @GetMapping("/{orderId}")
    public OrderDocument getOrder(@PathVariable String orderId) {
        return orderService.getOrderByOrderId(orderId);
    }

    // 取消订单
    @PostMapping("/{orderId}/cancel")
    public OrderDocument cancelOrder(@PathVariable String orderId) {
        return orderService.cancelOrder(orderId);
    }

    // 用于接收前端创建订单时传过来的body
    @Data
    public static class CreateOrderRequest {
        private String customerEmail;
        private List<OrderLineItem> items;
    }
}
