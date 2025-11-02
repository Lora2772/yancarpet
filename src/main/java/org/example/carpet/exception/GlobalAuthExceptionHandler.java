package org.example.carpet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalAuthExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String handleBadCred(BadCredentialsException ex) {
        return ex.getMessage(); // 前端直接显示 “Incorrect password”
    }
}
