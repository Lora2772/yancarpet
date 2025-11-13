package org.example.carpet.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String email) {
        super(String.format("Account not found with email: %s", email));
    }
}
