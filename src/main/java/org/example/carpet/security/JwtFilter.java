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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * 与 SecurityConfig 白名单保持一致（仅用于 shouldNotFilter 的快速跳过）
     */
    // 与 SecurityConfig 保持一致的白名单（仅在过滤器层用于“快速跳过”，真正的授权还是由 SecurityConfig 控）

    private static final List<AntPathRequestMatcher> PUBLIC_MATCHERS = List.of(
            new AntPathRequestMatcher("/"),
            new AntPathRequestMatcher("/index.html"),
            new AntPathRequestMatcher("/favicon.ico"),
            new AntPathRequestMatcher("/assets/**"),
            new AntPathRequestMatcher("/auth/**"),
            new AntPathRequestMatcher("/account/create"),
            new AntPathRequestMatcher("/items/**"),
            new AntPathRequestMatcher("/inventory/**"),
            new AntPathRequestMatcher("/media/**"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/actuator/health")
    );

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // 1) 预检请求直接跳过
        if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;
        // 2) 白名单端点直接跳过
        return PUBLIC_MATCHERS.stream().anyMatch(m -> m.matches(request));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        try {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            // 没有 Authorization 或不是 Bearer -> 放行（保持匿名态，由 SecurityConfig 决定是否允许）
            if (header == null || !header.startsWith("Bearer ")) {
                chain.doFilter(request, response);
                return;
            }

            String token = header.substring(7).trim();
            // 用你的 JwtUtil 解析 email
            String email = jwtUtil.validateAndExtractEmail(token);
            if (email == null || email.isBlank()) {
                // 非法 token -> 当匿名处理
                chain.doFilter(request, response);
                return;
            }

            // 注入一个带 ROLE_USER 的认证对象；如需更细权限可扩展为从 token / DB 取角色
            // 构造一个最简单的认证对象（无角色）
            var auth = new UsernamePasswordAuthenticationToken(
                    email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            // 写入 SecurityContext
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            // 不要中断请求，避免把所有错误变成 500/401；仅记录日志，按匿名继续
            log.warn("JWT parse error: {}", e.getMessage());
            // 出错也不要打断链路，按匿名继续，避免 500
        }

        chain.doFilter(request, response);
    }
}