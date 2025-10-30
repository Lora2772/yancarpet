package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.dto.LoginRequest;
import org.example.carpet.dto.LoginResponse;
import org.example.carpet.model.UserAccountDocument;
import org.example.carpet.security.JwtUtil;
import org.example.carpet.service.AccountService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AccountService accountService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        UserAccountDocument user = accountService.findByEmail(request.getEmail());
        if (user == null) {
            throw new RuntimeException("Invalid credentials");
        }
        if (!accountService.passwordMatches(user, request.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponse(token, "Login successful");
    }
}
