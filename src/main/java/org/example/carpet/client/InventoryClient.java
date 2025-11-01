package org.example.carpet.client;

import org.example.carpet.service.InventoryService;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for communicating with Inventory Service.
 * Currently points to the same app (localhost:8080), but in real deployment
 * it will call a separate microservice or container.
 */
@FeignClient(
        name = "inventory-service",
        url = "http://localhost:8080", // docker-compose内部可以改成 http://inventory:8080
        path = "/inventory"
)
public interface InventoryClient {

    public record ReservationResult(boolean success, String reservationId, int requested, int available) {}

    @GetMapping("/check")
    InventoryService.InventoryStatus checkInventory(@RequestParam("sku") String sku);

    @PostMapping("/reserve")
    ReservationResult reserve(@RequestParam("sku") String sku, @RequestParam("quantity") int quantity);

    @PostMapping("/release")
    void release(@RequestParam("reservationId") String reservationId, @RequestParam("quantity") int quantity);

}
