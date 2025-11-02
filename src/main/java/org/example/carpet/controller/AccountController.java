package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.dto.AccountCreateRequest;
import org.example.carpet.dto.AccountResponse;
import org.example.carpet.service.AccountService;
import org.springframework.http.ResponseEntity;
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
}
