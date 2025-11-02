package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.dto.LoginRequest;
import org.example.carpet.dto.LoginResp;
import org.example.carpet.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResp> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req.getEmail(), req.getPassword()));
    }
}
