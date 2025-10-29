package org.example.carpet.controller;

import lombok.RequiredArgsConstructor;
import org.example.carpet.dto.LoginRequest;
import org.example.carpet.dto.LoginResponse;
import org.example.carpet.security.JwtUtil;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        // Normally you'd verify credentials from DB.
        // Here we assume login always succeeds for simplicity.
        String token = jwtUtil.generateToken(request.getEmail());
        return new LoginResponse(token, "Login successful");
    }
}
