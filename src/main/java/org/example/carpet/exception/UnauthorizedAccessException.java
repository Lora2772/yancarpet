package org.example.carpet.exception;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String requesterEmail, String resource) {
        super(String.format("User %s is not authorized to access %s", requesterEmail, resource));
    }
}
