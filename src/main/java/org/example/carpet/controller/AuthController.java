package org.example.carpet.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.carpet.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<TokenResp> login(@RequestBody LoginReq req) {
        // TODO: 在这里校验用户名/密码（示例放行）
        String token = jwtUtil.generateToken(req.getEmail());
        return ResponseEntity.ok(new TokenResp(token));
    }

    @Data
    public static class LoginReq {
        private String email;
        private String password;
    }

    @Data
    public static class TokenResp {
        private final String token;
    }
}
