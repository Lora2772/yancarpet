package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.dto.AccountCreateRequest;
import org.example.carpet.dto.AccountUpdateRequest;
import org.example.carpet.dto.AccountResponse;
import org.example.carpet.service.AccountService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // Create Account (public)
    @PostMapping("/create")
    public AccountResponse create(@RequestBody AccountCreateRequest req) {
        return accountService.createAccount(req);
    }

    // Update Account (must be logged in)
    @PutMapping("/update")
    public AccountResponse update(@RequestBody AccountUpdateRequest req,
                                  Authentication auth) {
        String email = auth.getName();
        return accountService.updateAccount(email, req);
    }

    // Account Lookup (must be logged in)
    @GetMapping("/me")
    public AccountResponse me(Authentication auth) {
        String email = auth.getName();
        return accountService.getMyAccount(email);
    }
}
