package org.example.carpet.exception;

public class InsufficientStockException extends RuntimeException {
    private final String sku;
    private final int requested;
    private final int available;

    public InsufficientStockException(String sku, int requested, int available) {
        super("Insufficient stock: sku=" + sku + ", requested=" + requested + ", available=" + available);
        this.sku = sku; this.requested = requested; this.available = available;
    }
    public String getSku() { return sku; }
    public int getRequested() { return requested; }
    public int getAvailable() { return available; }
}
