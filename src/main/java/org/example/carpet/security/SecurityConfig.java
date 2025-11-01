package org.example.carpet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter; // 你的 JWT 过滤器

    // 明确列出开放 API（注意：没有使用 "**/*.js" 这类危险 pattern）
    private static final String[] PUBLIC_ENDPOINTS = {
            "/", "/index.html", "/favicon.ico",
            "/assets/**",               // 若有前端静态托管到后端（否则可删）
            "/auth/**", "/account/create",
            "/items/**", "/inventory/**", "/media/**",
            "/items/recommendations", "/items/*/recommendations",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // 放行常见静态资源目录（/static、/public、/resources、/META-INF/resources、/webjars）
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                        // 预检请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 你开放的业务端点
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // 其余均需要认证
                        .anyRequest().authenticated()
                )

                // 把 JWT 过滤器挂在 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // 统一异常返回（401/403）
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JwtAuthEntryPoint())
                        .accessDeniedHandler(new RestAccessDeniedHandler())
                );

        // 如果需要 H2 控制台可打开：
        // http.headers(h -> h.frameOptions(f -> f.disable()))
        //     .authorizeHttpRequests(auth -> auth.requestMatchers("/h2-console/**").permitAll());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // 前端本地开发端口（Vite）
        cfg.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // PathPattern 下这里用 "/**" 是安全的（不会再在 ** 后追加其他字符）
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
