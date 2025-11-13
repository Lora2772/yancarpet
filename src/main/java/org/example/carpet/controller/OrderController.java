package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.dto.AddressUpdateRequest;
import org.example.carpet.dto.CreateOrderRequest;
import org.example.carpet.dto.OrderUpdateRequest;
import org.example.carpet.exception.InvalidOrderStateException;
import org.example.carpet.exception.UnauthorizedAccessException;
import org.example.carpet.model.Address;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.service.OrderService;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Order lifecycle:
 * - POST /orders                  -> create (reserve inventory)
 * - GET  /orders/{orderId}        -> lookup order
 * - POST /orders/{orderId}/cancel -> cancel (release inventory)
 * - PUT  /orders/{orderId}        -> limited update
 *
 * Cassandra 扩展：
 * - GET  /orders/{orderId}/events -> 订单事件时间线（order_events_by_order）
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 为了少改 Service 的读取面，这里直接用 Template 读取事件表
    private final CassandraTemplate cassandraTemplate;

    // ====== 新增：订单历史 ======
    // GET /orders/history?page=0&size=20   （需登录）
    @GetMapping("/history")
    public Page<OrderDocument> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        String email = auth.getName();
        return orderService.getOrderHistory(email, page, size);
    }

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
        String callerEmail = auth.getName();
        OrderDocument order = orderService.getOrderByOrderId(orderId);

        if (!order.getCustomerEmail().equalsIgnoreCase(callerEmail)) {
            throw new UnauthorizedAccessException(callerEmail, "order " + orderId);
        }
        if (!"RESERVED".equalsIgnoreCase(order.getStatus())) {
            throw new InvalidOrderStateException(orderId, order.getStatus(), "RESERVED");
        }
        if (request.getCustomerEmailOverride() != null &&
                !request.getCustomerEmailOverride().isBlank()) {
            order.setCustomerEmail(request.getCustomerEmailOverride());
        }
        return orderService.saveDirect(order);
    }

    // ----- Update Shipping Address -----
    @PutMapping("/{orderId}/shipping-address")
    public OrderDocument updateShippingAddress(
            @PathVariable String orderId,
            @RequestBody AddressUpdateRequest request,
            Authentication auth
    ) {
        String callerEmail = auth.getName();

        // Convert DTO to Address model
        Address address = Address.builder()
                .line1(request.getLine1())
                .line2(request.getLine2())
                .city(request.getCity())
                .stateOrProvince(request.getStateOrProvince())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .build();

        return orderService.updateShippingAddress(orderId, callerEmail, address);
    }

    // -------------------- Cassandra：订单事件时间线 --------------------

    /** Cassandra：订单时间线（倒序），表：order_events_by_order(order_id, ts DESC, type, payload_json) */
    @GetMapping("/{orderId}/events")
    public List<Map<String, Object>> listOrderEvents(@PathVariable String orderId,
                                                     @RequestParam(required = false, defaultValue = "50") int limit) {
        String cql = "SELECT order_id, ts, type, payload_json FROM order_events_by_order " +
                "WHERE order_id = ? LIMIT " + Math.max(1, limit);
        return cassandraTemplate.getCqlOperations().query(cql, ps -> ps.bind(orderId), (row, i) -> Map.of(
                "orderId", row.getString("order_id"),
                "ts", row.getLong("ts"),
                "type", row.getString("type"),
                "payload", row.getString("payload_json")
        ));
    }

}
