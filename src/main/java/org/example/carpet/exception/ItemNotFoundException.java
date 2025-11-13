package org.example.carpet.exception;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String sku) {
        super(String.format("Item not found with SKU: %s", sku));
    }
}
