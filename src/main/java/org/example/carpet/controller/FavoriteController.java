package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.model.Favorite;
import org.example.carpet.service.FavoriteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService service;

    @GetMapping
    public List<Favorite> list(Authentication auth) {
        return service.list(auth.getName());
    }

    @GetMapping("/has/{sku}")
    public boolean has(@PathVariable String sku, Authentication auth) {
        return service.has(auth.getName(), sku);
    }

    @PostMapping("/{sku}")
    public void add(@PathVariable String sku, Authentication auth) {
        service.add(auth.getName(), sku);
    }

    @DeleteMapping("/{sku}")
    public void remove(@PathVariable String sku, Authentication auth) {
        service.remove(auth.getName(), sku);
    }

    @PostMapping("/{sku}/toggle")
    public boolean toggle(@PathVariable String sku, Authentication auth) {
        return service.toggle(auth.getName(), sku);
    }
}
