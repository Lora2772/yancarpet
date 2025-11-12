package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.dto.AccountCreateRequest;
import org.example.carpet.dto.AccountResponse;
import org.example.carpet.dto.AccountUpdateRequest;
import org.example.carpet.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/create")
    public ResponseEntity<AccountResponse> create(@RequestBody AccountCreateRequest req) {
        return ResponseEntity.ok(accountService.createAccount(req));
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount(Authentication auth) {
        String email = auth.getName();
        return ResponseEntity.ok(accountService.getMyAccount(email));
    }

    @PutMapping("/me")
    public ResponseEntity<AccountResponse> updateMyAccount(
            @RequestBody AccountUpdateRequest req,
            Authentication auth) {
        String email = auth.getName();
        return ResponseEntity.ok(accountService.updateAccount(email, req));
    }
}
