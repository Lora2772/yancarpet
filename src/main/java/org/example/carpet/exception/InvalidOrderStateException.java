package org.example.carpet.exception;

public class InvalidOrderStateException extends RuntimeException {
    public InvalidOrderStateException(String orderId, String currentState, String expectedState) {
        super(String.format("Order %s is in state '%s', but expected '%s'", orderId, currentState, expectedState));
    }
}
