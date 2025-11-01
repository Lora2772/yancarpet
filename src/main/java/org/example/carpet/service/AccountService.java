package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.dto.AccountCreateRequest;
import org.example.carpet.dto.AccountUpdateRequest;
import org.example.carpet.dto.AccountResponse;
import org.example.carpet.model.UserAccountDocument;
import org.example.carpet.repository.mongo.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserAccountRepository userRepo;

    // Register
    public AccountResponse createAccount(AccountCreateRequest req) {
        // reject duplicate email
        userRepo.findByEmailIgnoreCase(req.getEmail()).ifPresent(u -> {
            throw new RuntimeException("Email already in use");
        });

        UserAccountDocument doc = UserAccountDocument.builder()
                .email(req.getEmail())
                .userName(req.getUserName())
                .passwordHash(hash(req.getPassword()))
                .shippingAddress(req.getShippingAddress())
                .billingAddress(req.getBillingAddress())
                .defaultPaymentMethod(req.getDefaultPaymentMethod())
                .build();

        userRepo.save(doc);
        return toResponse(doc);
    }

    // Update my profile
    public AccountResponse updateAccount(String emailFromToken, AccountUpdateRequest req) {
        UserAccountDocument doc = userRepo.findByEmailIgnoreCase(emailFromToken)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (req.getUserName() != null) {
            doc.setUserName(req.getUserName());
        }
        if (req.getShippingAddress() != null) {
            doc.setShippingAddress(req.getShippingAddress());
        }
        if (req.getBillingAddress() != null) {
            doc.setBillingAddress(req.getBillingAddress());
        }
        if (req.getDefaultPaymentMethod() != null) {
            doc.setDefaultPaymentMethod(req.getDefaultPaymentMethod());
        }

        userRepo.save(doc);
        return toResponse(doc);
    }

    // Lookup my profile
    public AccountResponse getMyAccount(String emailFromToken) {
        UserAccountDocument doc = userRepo.findByEmailIgnoreCase(emailFromToken)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return toResponse(doc);
    }

    // Used by AuthController login to fetch user
    public UserAccountDocument findByEmail(String email) {
        return userRepo.findByEmailIgnoreCase(email).orElse(null);
    }

    // Used by AuthController login to verify password
    public boolean passwordMatches(UserAccountDocument doc, String rawPassword) {
        if (doc == null) return false;
        return doc.getPasswordHash().equals("{plain}" + rawPassword);
    }

    private String hash(String raw) {
        // simplest possible "hash" for demo.
        // replace with BCryptPasswordEncoder for production.
        return "{plain}" + raw;
    }

    private AccountResponse toResponse(UserAccountDocument doc) {
        return AccountResponse.builder()
                .email(doc.getEmail())
                .userName(doc.getUserName())
                .shippingAddress(doc.getShippingAddress())
                .billingAddress(doc.getBillingAddress())
                .defaultPaymentMethod(doc.getDefaultPaymentMethod())
                .build();
    }
}
