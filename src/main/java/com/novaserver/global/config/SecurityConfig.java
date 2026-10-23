package com.novaserver.global.config;

import com.novaserver.auth.jwt.JwtAccessDeniedHandler;
import com.novaserver.auth.jwt.JwtAuthenticationEntryPoint;
import com.novaserver.auth.jwt.JwtAuthenticationFilter;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.repository.AccessTokenRepository;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.clientpolicy.ClientPolicy;
import com.novaserver.clientpolicy.ClientPolicyFilter;
import com.novaserver.clientpolicy.ClientPolicySource;
import com.novaserver.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT 기반 보안 설정.
 *
 * <p>로그인·헬스체크·Swagger·에러 경로는 토큰 없이 허용하고, 관리자 경로는 ADMIN 권한이 필요하다. 도감 관리·공지 등록 경로는 WEB 토큰도 요구하며 나머지는
 * 유효한 access 토큰이 있어야 한다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/api/auth/**",
        "/api/client/status",
        "/actuator/health",
        "/error",
        "/api/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    private final JwtProvider jwtProvider;
    private final TokenBlocklistRepository tokenBlocklistRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final ObjectProvider<ClientPolicySource> clientPolicy;

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
                                        .requestMatchers("/api/admin/codex/**")
                                        .access(
                                                AuthorizationManagers.allOf(
                                                        AuthorityAuthorizationManager.hasRole(
                                                                Role.ADMIN.name()),
                                                        AuthorityAuthorizationManager.hasAuthority(
                                                                "CLIENT_WEB")))
                                        .requestMatchers("/api/admin/notices/**")
                                        .access(
                                                AuthorizationManagers.allOf(
                                                        AuthorityAuthorizationManager.hasRole(
                                                                Role.ADMIN.name()),
                                                        AuthorityAuthorizationManager.hasAuthority(
                                                                "CLIENT_WEB")))
                                        .requestMatchers("/api/admin/mails/**")
                                        .access(
                                                AuthorizationManagers.allOf(
                                                        AuthorityAuthorizationManager.hasRole(
                                                                Role.ADMIN.name()),
                                                        AuthorityAuthorizationManager.hasAuthority(
                                                                "CLIENT_WEB")))
                                        .requestMatchers("/api/admin/users/**")
                                        .access(
                                                AuthorizationManagers.allOf(
                                                        AuthorityAuthorizationManager.hasRole(
                                                                Role.ADMIN.name()),
                                                        AuthorityAuthorizationManager.hasAuthority(
                                                                "CLIENT_WEB")))
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
                        new JwtAuthenticationFilter(
                                jwtProvider, tokenBlocklistRepository, accessTokenRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(
                        new ClientPolicyFilter(
                                () -> {
                                    ClientPolicySource source = clientPolicy.getIfAvailable();
                                    return source == null
                                            ? ClientPolicy.disabled()
                                            : source.current();
                                }),
                        JwtAuthenticationFilter.class);
        return http.build();
    }
}
