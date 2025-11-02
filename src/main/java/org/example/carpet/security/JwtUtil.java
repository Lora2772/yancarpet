package org.example.carpet.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret:change-this-secret}")
    private String secret;

    @Value("${app.jwt.ttlSec:86400}") // 默认 1 天
    private long ttlSec;

    public String generateToken(String email) {
        long exp = Instant.now().getEpochSecond() + ttlSec;
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"sub\":\"" + email + "\",\"exp\":" + exp + "}");
        String signingInput = header + "." + payload;
        String sig = hmacSha256(signingInput, secret);
        return signingInput + "." + sig;
    }

    /** 验证成功返回 email；失败返回 null */
    public String validateAndExtractEmail(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String header = parts[0], payload = parts[1], sig = parts[2];
            String signingInput = header + "." + payload;

            String expected = hmacSha256(signingInput, secret);
            if (!constantTimeEq(sig, expected)) return null;

            String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            String sub = getJsonString(json, "sub");
            long exp = getJsonLong(json, "exp");
            if (sub == null || exp < Instant.now().getEpochSecond()) return null;
            return sub;
        } catch (Exception e) {
            return null;
        }
    }

    private static String base64Url(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean constantTimeEq(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

    // 极简 JSON 解析（因为字段固定）
    private static String getJsonString(String json, String key) {
        String pat = "\"" + key + "\":\"";
        int i = json.indexOf(pat);
        if (i < 0) return null;
        int s = i + pat.length();
        int e = json.indexOf("\"", s);
        if (e < 0) return null;
        return json.substring(s, e);
    }

    private static long getJsonLong(String json, String key) {
        String pat = "\"" + key + "\":";
        int i = json.indexOf(pat);
        if (i < 0) return -1;
        int s = i + pat.length();
        int e = s;
        while (e < json.length() && Character.isDigit(json.charAt(e))) e++;
        return Long.parseLong(json.substring(s, e));
    }
}
