package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.dto.CartChangeQtyRequest;
import org.example.carpet.dto.CartItemView;
import org.example.carpet.dto.CartUpsertRequest;
import org.example.carpet.service.CartService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public List<CartItemView> list(Authentication auth) {
        String email = auth.getName();
        return cartService.list(email);
    }

    @PostMapping("/add")
    public void add(@RequestBody CartUpsertRequest req, Authentication auth) {
        String email = auth.getName();
        cartService.upsert(email, req);
    }

    @PutMapping("/qty")
    public void changeQty(@RequestBody CartChangeQtyRequest req, Authentication auth) {
        String email = auth.getName();
        cartService.changeQty(email, req);
    }

    @DeleteMapping("/item/{sku}")
    public void remove(@PathVariable String sku, Authentication auth) {
        String email = auth.getName();
        cartService.remove(email, sku);
    }

    @DeleteMapping("/clear")
    public void clear(Authentication auth) {
        String email = auth.getName();
        cartService.clear(email);
    }
}
