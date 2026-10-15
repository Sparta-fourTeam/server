package com.novaserver.global.config;

import com.novaserver.auth.jwt.JwtAccessDeniedHandler;
import com.novaserver.auth.jwt.JwtAuthenticationEntryPoint;
import com.novaserver.auth.jwt.JwtAuthenticationFilter;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT 기반 보안 설정.
 *
 * <p>로그인·헬스체크·Swagger·에러 경로는 토큰 없이 허용하고, 관리자 경로는 WEB 토큰, 나머지는 유효한 access 토큰이 있어야 한다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/api/auth/**",
        "/actuator/health",
        "/error",
        "/api/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    private final JwtProvider jwtProvider;
    private final TokenBlocklistRepository tokenBlocklistRepository;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.DELETE, "/api/auth/me")
                                        .authenticated()
                                        .requestMatchers(HttpMethod.POST, "/api/auth/social/link")
                                        .authenticated()
                                        .requestMatchers(PUBLIC_PATHS)
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasRole(Role.ADMIN.name())
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        exception ->
                                exception
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, tokenBlocklistRepository),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
