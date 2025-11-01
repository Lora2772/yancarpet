package org.example.carpet.api;

import org.example.carpet.exception.InsufficientStockException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String,Object> handleInsufficient(InsufficientStockException ex){
        return Map.of("error","INSUFFICIENT_STOCK","message",ex.getMessage(),
                "sku",ex.getSku(),"requested",ex.getRequested(),"available",ex.getAvailable(),
                "timestamp",Instant.now().toString());
    }
}

