package com.novaserver.global.config;

import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.JwtAccessDeniedHandler;
import com.novaserver.auth.jwt.JwtAuthenticationEntryPoint;
import com.novaserver.auth.jwt.JwtAuthenticationFilter;
import com.novaserver.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    // TODO: 게스트 로그인(#1) 완료 후 제거. 로그인 API가 없어 토큰을 받을 수 없는 동안 기존 API를 임시로 연다
    private static final String[] TEMP_PUBLIC_PATHS = {"/api/battle/**", "/api/data/**"};

    private final JwtProvider jwtProvider;
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
                                auth.requestMatchers(PUBLIC_PATHS)
                                        .permitAll()
                                        .requestMatchers(TEMP_PUBLIC_PATHS)
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasRole(ClientType.WEB.name())
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        exception ->
                                exception
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
