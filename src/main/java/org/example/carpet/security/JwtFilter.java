package org.example.carpet.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.util.Collections;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JwtFilter：整合现有的 JwtUtil（Base64 token：email:timestamp）。
 * 规则：
 * - 白名单与 OPTIONS 直接放行；
 * - 没有 Authorization 头 -> 放行；
 * - 有 Bearer token -> 用 JwtUtil 解析 email 成功则注入 SecurityContext，失败则当匿名放行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // 与 SecurityConfig 保持一致的白名单（仅在过滤器层用于“快速跳过”，真正的授权还是由 SecurityConfig 控）
    private static final List<AntPathRequestMatcher> PUBLIC_MATCHERS = List.of(
            new AntPathRequestMatcher("/auth/**"),
            new AntPathRequestMatcher("/account/create"),
            new AntPathRequestMatcher("/items/**"),
            new AntPathRequestMatcher("/inventory/**"),
            new AntPathRequestMatcher("/media/**"),
            new AntPathRequestMatcher("/"),
            new AntPathRequestMatcher("/index.html"),
            new AntPathRequestMatcher("/favicon.ico"),
            new AntPathRequestMatcher("/assets/**")
    );

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // 1) 预检请求直接跳过
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        // 2) 白名单端点直接跳过
        for (var m : PUBLIC_MATCHERS) {
            if (m.matches(request)) {
                return true;
            }
        }
        // 其余路径交给 doFilterInternal 判断是否带 token
        return false;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            // 没有 Authorization 或不是 Bearer -> 放行（保持匿名态，由 SecurityConfig 决定是否允许）
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring("Bearer ".length()).trim();
            if (token.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            // 用你的 JwtUtil 解析 email
            String email = jwtUtil.validateAndExtractEmail(token);
            if (email == null || email.isBlank()) {
                // 解析失败，不抛异常，按匿名继续
                log.debug("JWT parse failed or empty email, path={}, method={}", request.getRequestURI(), request.getMethod());
                filterChain.doFilter(request, response);
                return;
            }

            // 构造一个最简单的认证对象（无角色）
            List<GrantedAuthority> authorities = Collections.emptyList();
            var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);

            // 写入 SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            // 不要中断请求，避免把所有错误变成 500/401；仅记录日志，按匿名继续
            log.warn("JWT filter error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
