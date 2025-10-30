package org.example.carpet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    // 注意：不要把 "/" 放进白名单，否则全命中
    private static final List<String> WHITELIST_PREFIXES = List.of(
            "/auth",           // 前缀匹配
            "/account/create",
            "/media",
            "/items",
            "/index.html",
            "/assets"
    );

    private static boolean isWhitelisted(String path) {
        // 用 getServletPath() 得到的是不带 query 的路径
        return WHITELIST_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String method = request.getMethod();
        final String path   = request.getServletPath();

        // 1) 预检请求放行（CORS）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 2) 白名单直通（无条件）
        if (isWhitelisted(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 3) 非白名单：没有 Bearer 就不设置认证，交给 SecurityConfig 的 authenticated() 去处理（通常 401）
        final String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        // 4) （开发版）假认证：有 Bearer 就给个 ROLE_USER。真正上线时在这里校验/解析 JWT。
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
                "demo@yan.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
        SecurityContextHolder.setContext(ctx);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
