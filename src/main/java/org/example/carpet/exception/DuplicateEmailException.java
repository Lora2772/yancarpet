package org.example.carpet.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super(String.format("Account already exists with email: %s", email));
    }
}
