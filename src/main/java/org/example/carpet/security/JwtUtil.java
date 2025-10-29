package org.example.carpet.security;

import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class JwtUtil {

    public String generateToken(String email) {
        // simplified fake token (email + timestamp -> base64)
        String payload = email + ":" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(payload.getBytes());
    }

    public String validateAndExtractEmail(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            return decoded.split(":")[0];
        } catch (Exception e) {
            return null;
        }
    }
}
