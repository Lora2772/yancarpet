package org.example.carpet.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String orderId) {
        super(String.format("Payment not found for order: %s", orderId));
    }
}
