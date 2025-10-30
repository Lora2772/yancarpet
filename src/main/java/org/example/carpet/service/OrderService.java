package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.client.InventoryClient;
import org.example.carpet.kafka.InventoryEventProducer;
import org.example.carpet.model.OrderDocument;
import org.example.carpet.model.OrderLineItem;
import org.example.carpet.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Handles order lifecycle using Feign to call Inventory Service.
 *
 *  * Handles order lifecycle:
 *  * - create (reserve inventory)
 *  * - get
 *  * - cancel (release inventory)
 *  *
 */

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient; // Feign client
    private final InventoryEventProducer inventoryEventProducer; // Kafka producer

    public OrderDocument createOrder(String customerEmail, List<OrderLineItem> items) {

        // 1. Try reserving inventory for each item via InventoryClient
        for (OrderLineItem line : items) {
            boolean ok = inventoryClient.reserve(line.getSku(), line.getQuantity());
            if (!ok) {
                rollbackReservations(items, line);
                throw new RuntimeException("Insufficient stock for sku=" + line.getSku());
            }
        }

        // 2. Compute total
        double total = items.stream()
                .mapToDouble(li -> li.getPrice() * li.getQuantity())
                .sum();

        // 3. Generate business orderId
        String orderId = "ORD-" + UUID.randomUUID();

        // 4. Build order
        OrderDocument order = OrderDocument.builder()
                .orderId(orderId)
                .customerEmail(customerEmail)
                .items(items)
                .totalAmount(total)
                .status("RESERVED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 5. Persist
        OrderDocument saved = orderRepository.save(order);

        // 6. Emit Kafka event: InventoryReserved for each line
        for (OrderLineItem line : items) {
            inventoryEventProducer.publishInventoryReserved(
                    orderId,
                    line.getSku(),
                    line.getQuantity()
            );
        }

        return saved;
    }

    private void rollbackReservations(List<OrderLineItem> items, OrderLineItem failedLine) {
        for (OrderLineItem line : items) {
            if (line == failedLine) break;
            inventoryClient.release(line.getSku(), line.getQuantity());
        }
    }

    public OrderDocument getOrderByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    public OrderDocument cancelOrder(String orderId) {
        OrderDocument order = getOrderByOrderId(orderId);
        if (!order.getStatus().equals("RESERVED") && !order.getStatus().equals("PAID")) {
            return order;
        }

        for (OrderLineItem line : order.getItems()) {
            inventoryClient.release(line.getSku(), line.getQuantity());
            // emit inventory released event
            inventoryEventProducer.publishInventoryReleased(
                    orderId,
                    line.getSku(),
                    line.getQuantity()
            );
        }

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public OrderDocument markPaid(String orderId) {
        OrderDocument order = getOrderByOrderId(orderId);
        if (!order.getStatus().equals("RESERVED")) {
            throw new RuntimeException("Order not in RESERVED state, can't mark PAID.");
        }

        order.setStatus("PAID");
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    // allow other services (like PaymentService.refundPayment) to persist status changes
    public OrderDocument saveDirect(OrderDocument order) {
        order.setUpdatedAt(java.time.LocalDateTime.now());
        return orderRepository.save(order);
    }

}
