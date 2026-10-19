package com.novaserver.auth.jwt;

import com.novaserver.auth.repository.AccessTokenRepository;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authorization 헤더의 access 토큰을 검증하고, 통과하면 인증 정보를 SecurityContext에 넣는다.
 *
 * <p>검증에 실패해도 여기서 응답하지 않는다. 실패 원인을 요청 속성 {@link #ERROR_ATTRIBUTE}에 남기고 다음 필터로 넘긴다. 인증이 필요한 경로라면
 * AuthenticationEntryPoint가 이 원인으로 401을 응답한다.
 *
 * <p>GAME 클라이언트는 계정당 세션 1개만 허용한다. 더 최신 로그인이 {@link AccessTokenRepository}의 값을 덮어쓰면, 서명과 만료가 유효해도 이전
 * access 토큰은 거부된다. 이미 맺어진 WebSocket 연결은 이 체크를 다시 타지 않으므로, 그 연결을 끊는 건 {@link
 * GameWebSocketSessionRegistry}가 로그인 시점에 직접 한다.
 *
 * <p>{@code @Component}로 등록하지 않는다. 서블릿 필터로도 자동 등록되어 두 번 실행되기 때문이다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 토큰 검증 실패 원인({@link ResponseStatusException})을 담는 요청 속성 이름. */
    public static final String ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".ERROR";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final TokenBlocklistRepository tokenBlocklistRepository;
    private final AccessTokenRepository accessTokenRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(request, header.substring(BEARER_PREFIX.length()));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        TokenInfo tokenInfo;
        try {
            tokenInfo = jwtProvider.parse(token);

            log.info("JWT userId: {}", tokenInfo.userId());
            log.info("JWT role: {}", tokenInfo.role());
            log.info("JWT tokenType: {}", tokenInfo.tokenType());

        } catch (ResponseStatusException e) {
            request.setAttribute(ERROR_ATTRIBUTE, e);
            return;
        }

        if (tokenInfo.tokenType() != TokenType.ACCESS) {
            request.setAttribute(ERROR_ATTRIBUTE, ErrorCode.INVALID_TOKEN.exception());
            return;
        }

        if (isSupersededGameSession(tokenInfo)) {
            request.setAttribute(ERROR_ATTRIBUTE, ErrorCode.INVALID_TOKEN.exception());
            return;
        }

        if (tokenBlocklistRepository.isBlocked(tokenInfo.userId())) {
            request.setAttribute(ERROR_ATTRIBUTE, ErrorCode.ACCOUNT_DELETED.exception());
            return;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(toAuthentication(tokenInfo));
        SecurityContextHolder.setContext(context);
    }

    /** GAME 클라이언트는 계정당 세션 1개만 유효하다. 더 최신 로그인으로 교체된 access 토큰이면 true. */
    private boolean isSupersededGameSession(TokenInfo tokenInfo) {
        if (tokenInfo.clientType() != ClientType.GAME) {
            return false;
        }
        return accessTokenRepository
                .findTokenId(tokenInfo.userId(), ClientType.GAME)
                .filter(storedTokenId -> !storedTokenId.equals(tokenInfo.tokenId()))
                .isPresent();
    }

    private Authentication toAuthentication(TokenInfo tokenInfo) {
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + tokenInfo.role().name());
        return new UsernamePasswordAuthenticationToken(
                tokenInfo.userId(),
                null,
                List.of(
                        authority,
                        new SimpleGrantedAuthority("CLIENT_" + tokenInfo.clientType().name())));
    }
}
