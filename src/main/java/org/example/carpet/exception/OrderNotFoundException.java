package org.example.carpet.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String orderId) {
        super(String.format("Order not found: %s", orderId));
    }
}
