package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.dto.AccountCreateRequest;
import org.example.carpet.dto.AccountUpdateRequest;
import org.example.carpet.dto.AccountResponse;
import org.example.carpet.model.Account;
import org.example.carpet.repository.mongo.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository userRepo;
    private final PasswordEncoder encoder; // 确保只定义了一个 Bean：BCryptPasswordEncoder

    // ---------- Register ----------
    public AccountResponse createAccount(AccountCreateRequest req) {
        final String email = normalizeEmail(req.getEmail());

        userRepo.findByEmail(email).ifPresent(u -> {
            throw new RuntimeException("Email already in use");
        });

        Account doc = Account.builder()
                .email(email)
                .userName(req.getUserName())
                .passwordHash(encoder.encode(req.getPassword())) // 关键：BCrypt
                .shippingAddress(req.getShippingAddress())
                .billingAddress(req.getBillingAddress())
                .defaultPaymentMethod(req.getDefaultPaymentMethod())
                .build();

        userRepo.save(doc);
        return toResponse(doc);
    }

    // ---------- Update my profile (不改密码) ----------
    public AccountResponse updateAccount(String emailFromToken, AccountUpdateRequest req) {
        Account doc = userRepo.findByEmail(normalizeEmail(emailFromToken))
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (req.getUserName() != null)            doc.setUserName(req.getUserName());
        if (req.getShippingAddress() != null)     doc.setShippingAddress(req.getShippingAddress());
        if (req.getBillingAddress() != null)      doc.setBillingAddress(req.getBillingAddress());
        if (req.getDefaultPaymentMethod() != null) doc.setDefaultPaymentMethod(req.getDefaultPaymentMethod());

        userRepo.save(doc);
        return toResponse(doc);
    }

    // ---------- Lookup ----------
    public AccountResponse getMyAccount(String emailFromToken) {
        Account doc = userRepo.findByEmail(normalizeEmail(emailFromToken))
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return toResponse(doc);
    }

    // ---------- For AuthController ----------
    public Account findByEmail(String email) {
        return userRepo.findByEmail(normalizeEmail(email)).orElse(null);
    }

    /**
     * 校验口令；兼容旧数据：
     *  - 如果库里是 "{plain}xxx"：先比对明文；成功则立刻迁移成 BCrypt，并保存回库
     *  - 否则用 encoder.matches(raw, encoded)
     */
    public boolean passwordMatches(Account doc, String rawPassword) {
        if (doc == null) return false;

        String stored = doc.getPasswordHash();
        if (stored == null) return false;

        // 旧数据迁移路径
        if (stored.startsWith("{plain}")) {
            String plain = stored.substring("{plain}".length());
            if (!plain.equals(rawPassword)) return false;

            // 明文匹配成功 -> 迁移成 BCrypt
            String newHash = encoder.encode(rawPassword);
            doc.setPasswordHash(newHash);
            userRepo.save(doc);
            return true;
        }

        // 正常 BCrypt
        return encoder.matches(rawPassword, stored);
    }

    // ---------- Helpers ----------
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private AccountResponse toResponse(Account doc) {
        return AccountResponse.builder()
                .email(doc.getEmail())
                .userName(doc.getUserName())
                .shippingAddress(doc.getShippingAddress())
                .billingAddress(doc.getBillingAddress())
                .defaultPaymentMethod(doc.getDefaultPaymentMethod())
                .build();
    }
}
