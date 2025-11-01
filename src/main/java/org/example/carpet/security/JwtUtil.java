package org.example.carpet.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * 简易 JWT：token 结构为 base64Url(email) + "." + expEpochSec + "." + base64Url(HMACSHA256(payload))
 * payload = base64Url(email) + "." + exp
 */
@Component
public class JwtUtil {

    public String generateToken(String email) {
        String payload = email + ":" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    /** 验证签名与过期，成功返回 email，失败返回 null */
    public String validateAndExtractEmail(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            return decoded.split(":")[0];
        } catch (Exception e) {
            return null;
        }
    }
}

