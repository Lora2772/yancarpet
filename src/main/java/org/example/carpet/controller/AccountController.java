package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.dto.AccountCreateRequest;
import org.example.carpet.dto.AccountUpdateRequest;
import org.example.carpet.dto.AccountResponse;
import org.example.carpet.service.AccountService;
import org.example.carpet.service.ItemService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 账号相关接口：
 * - POST /account/create            创建账号（公开）
 * - PUT  /account/update            更新账号（需登录）
 * - GET  /account/me                查看账号（需登录）
 *
 * Cassandra 扩展：购物车（按用户分区 + 行级 TTL）
 * - PUT    /account/cart/items      upsert 一条购物车项（支持可选 ttlMinutes）
 * - GET    /account/cart/items      拉取购物车
 * - DELETE /account/cart/items/{sku}删除某个 sku
 * - DELETE /account/cart            清空购物车
 */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // Cassandra 购物车落在 ItemService 中（不新建类）
    private final ItemService itemService;

    // ---------- 账号 ----------
    @PostMapping("/create")
    public AccountResponse create(@RequestBody AccountCreateRequest req) {
        return accountService.createAccount(req);
    }

    @PutMapping("/update")
    public AccountResponse update(@RequestBody AccountUpdateRequest req,
                                  Authentication auth) {
        String email = auth.getName();
        return accountService.updateAccount(email, req);
    }

    @GetMapping("/me")
    public AccountResponse me(Authentication auth) {
        String email = auth.getName();
        return accountService.getMyAccount(email);
    }

    // ---------- Cassandra：购物车 ----------

    /** upsert 购物车项（默认 TTL=30 天；若 qty<=0 你也可以改成删除） */
    @PutMapping("/cart/items")
    public Map<String, Object> cartUpsert(Authentication auth,
                                          @RequestParam String sku,
                                          @RequestParam int qty,
                                          @RequestParam BigDecimal price,
                                          @RequestParam(required = false, defaultValue = "43200") int ttlMinutes) {
        String email = auth.getName();
        itemService.cartUpsertCassandra(email, sku, qty, price, Duration.ofMinutes(ttlMinutes));
        return Map.of("ok", true, "email", email, "sku", sku, "qty", qty, "price", price, "ttlMinutes", ttlMinutes);
    }

    /** 获取购物车 */
    @GetMapping("/cart/items")
    public List<ItemService.CartItemView> cartList(Authentication auth) {
        String email = auth.getName();
        return itemService.cartListCassandra(email);
    }

    /** 删除某个 sku */
    @DeleteMapping("/cart/items/{sku}")
    public Map<String, Object> cartRemove(Authentication auth, @PathVariable String sku) {
        String email = auth.getName();
        itemService.cartRemoveCassandra(email, sku);
        return Map.of("ok", true, "email", email, "sku", sku);
    }

    /** 清空购物车 */
    @DeleteMapping("/cart")
    public Map<String, Object> cartClear(Authentication auth) {
        String email = auth.getName();
        itemService.cartClearCassandra(email);
        return Map.of("ok", true, "email", email);
    }
}
