package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.dto.LoginResp;
import org.example.carpet.model.Account;
import org.example.carpet.repository.mongo.AccountRepository;
import org.example.carpet.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** 登录：校验邮箱 + BCrypt 密码，返回包含 JWT 的响应 */
    public LoginResp login(String email, String rawPassword) {
        Account acc = accountRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Incorrect Password"));

        if (acc.getPasswordHash() == null ||
                !passwordEncoder.matches(rawPassword, acc.getPasswordHash())) {
            throw new BadCredentialsException("Incorrect Password");
        }

        String token = jwtUtil.generateToken(acc.getEmail());
        return new LoginResp(token);
    }
}
