package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.OrderLineItem;
import org.example.carpet.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Handles order lifecycle:
 * - create (reserve inventory)
 * - get
 * - cancel (release inventory)
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    // 创建订单（从购物车/结账时调用）
    public OrderDocument createOrder(String customerEmail, List<OrderLineItem> items) {
        // 1. 校验所有库存 & 锁库存
        for (OrderLineItem line : items) {
            boolean ok = inventoryService.reserve(line.getSku(), line.getQuantity());
            if (!ok) {
                // 如果有任何一个SKU锁库存失败，则回滚之前锁的库存
                rollbackReservations(items, line);
                throw new RuntimeException("Insufficient stock for sku=" + line.getSku());
            }
        }

        // 2. 计算总价
        double total = items.stream()
                .mapToDouble(li -> li.getPrice() * li.getQuantity())
                .sum();

        // 3. 生成业务订单号
        String orderId = "ORD-" + UUID.randomUUID();

        // 4. 构建订单对象
        OrderDocument order = OrderDocument.builder()
                .orderId(orderId)
                .customerEmail(customerEmail)
                .items(items)
                .totalAmount(total)
                .status("RESERVED") // 已锁库存, 等待支付
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 5. 保存到 MongoDB
        return orderRepository.save(order);
    }

    // 回滚库存（仅在 createOrder 阶段用）
    private void rollbackReservations(List<OrderLineItem> items, OrderLineItem failedLine) {
        for (OrderLineItem line : items) {
            // 把已经reserve的都释放回去，直到失败的那个为止
            if (line == failedLine) break;
            inventoryService.release(line.getSku(), line.getQuantity());
        }
    }

    // 查订单（给用户或支付服务）
    public OrderDocument getOrderByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    // 取消订单（例如用户取消 or 支付失败）
    public OrderDocument cancelOrder(String orderId) {
        OrderDocument order = getOrderByOrderId(orderId);

        if (!order.getStatus().equals("RESERVED") && !order.getStatus().equals("PAID")) {
            // 如果订单已经取消过了，就直接返回
            return order;
        }

        // 释放库存
        for (OrderLineItem line : order.getItems()) {
            inventoryService.release(line.getSku(), line.getQuantity());
        }

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    // 标记订单支付成功（由 PaymentService 调用）
    public OrderDocument markPaid(String orderId) {
        OrderDocument order = getOrderByOrderId(orderId);
        if (!order.getStatus().equals("RESERVED")) {
            throw new RuntimeException("Order not in RESERVED state, can't mark PAID.");
        }

        order.setStatus("PAID");
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }
}
