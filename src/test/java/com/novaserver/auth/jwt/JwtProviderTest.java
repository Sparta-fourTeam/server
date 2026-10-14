package com.novaserver.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.novaserver.auth.jwt.TokenValidityProperties.Validity;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class JwtProviderTest {

    private static final String SECRET =
            "test-secret-key-test-secret-key-0123456789"; // gitleaks:allow

    private static final TokenValidityProperties VALIDITY =
            new TokenValidityProperties(
                    new Validity(Duration.ofMinutes(15), Duration.ofDays(30)),
                    new Validity(Duration.ofMinutes(15), Duration.ofHours(1)));

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, VALIDITY);

    @Test
    @DisplayName("access 토큰을 파싱하면 발급 때 넣은 userId·role·clientType·ACCESS 타입이 나온다")
    void shouldParseAccessToken_withIssuedValues() {
        // given
        String token = jwtProvider.createAccessToken(42L, ClientType.GAME, Role.USER);

        // when
        TokenInfo tokenInfo = jwtProvider.parse(token);

        // then
        assertThat(tokenInfo.userId()).isEqualTo(42L);
        assertThat(tokenInfo.role()).isEqualTo(Role.USER);
        assertThat(tokenInfo.clientType()).isEqualTo(ClientType.GAME);
        assertThat(tokenInfo.tokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(tokenInfo.tokenId()).isNotBlank();
    }

    @Test
    @DisplayName("refresh 토큰을 파싱하면 발급 때 받은 tokenId(jti)와 같은 값이 나온다")
    void shouldParseRefreshToken_withSameTokenIdAsIssued() {
        // given
        IssuedToken issued = jwtProvider.createRefreshToken(7L, ClientType.WEB, Role.ADMIN);

        // when
        TokenInfo tokenInfo = jwtProvider.parse(issued.token());

        // then
        assertThat(tokenInfo.tokenId()).isEqualTo(issued.tokenId());
        assertThat(tokenInfo.userId()).isEqualTo(7L);
        assertThat(tokenInfo.role()).isEqualTo(Role.ADMIN);
        assertThat(tokenInfo.clientType()).isEqualTo(ClientType.WEB);
        assertThat(tokenInfo.tokenType()).isEqualTo(TokenType.REFRESH);
    }

    @Test
    @DisplayName("refresh 토큰은 발급할 때마다 tokenId가 다르다")
    void shouldIssueDifferentTokenId_eachTime() {
        // when
        IssuedToken first = jwtProvider.createRefreshToken(42L, ClientType.GAME, Role.USER);
        IssuedToken second = jwtProvider.createRefreshToken(42L, ClientType.GAME, Role.USER);

        // then
        assertThat(first.tokenId()).isNotEqualTo(second.tokenId());
    }

    @Test
    @DisplayName("refresh 유효기간은 clientType별 설정을 따른다 (GAME 30일, WEB 1시간)")
    void shouldUseValidityOfClientType() {
        // when
        Duration game = lifetime(jwtProvider.createRefreshToken(1L, ClientType.GAME, Role.USER));
        Duration web = lifetime(jwtProvider.createRefreshToken(1L, ClientType.WEB, Role.USER));

        // then
        assertThat(game).isEqualTo(Duration.ofDays(30));
        assertThat(web).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("만료된 토큰이면 TOKEN_EXPIRED 예외를 던진다")
    void shouldThrowTokenExpired_whenTokenIsExpired() {
        // given
        Validity expired = new Validity(Duration.ofSeconds(-60), Duration.ofSeconds(-60));
        String token =
                new JwtProvider(SECRET, new TokenValidityProperties(expired, expired))
                        .createAccessToken(42L, ClientType.GAME, Role.USER);

        // when & then
        assertThatThrownBy(() -> jwtProvider.parse(token))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("reason", ErrorCode.TOKEN_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("형식이 깨진 토큰이면 INVALID_TOKEN 예외를 던진다")
    void shouldThrowInvalidToken_whenTokenIsMalformed() {
        // when & then
        assertThatThrownBy(() -> jwtProvider.parse("not-a-jwt"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("reason", ErrorCode.INVALID_TOKEN.getMessage());
    }

    @Test
    @DisplayName("role 클레임이 없는 토큰(role 도입 전 발급)이면 500이 아니라 INVALID_TOKEN을 던진다")
    void shouldThrowInvalidToken_whenRoleClaimMissing() {
        // given
        Date now = new Date();
        String token =
                Jwts.builder()
                        .id("legacy-jti")
                        .subject("42")
                        .audience()
                        .add(ClientType.GAME.name())
                        .and()
                        .claim("type", TokenType.REFRESH.name())
                        .issuedAt(now)
                        .expiration(new Date(now.getTime() + 60_000))
                        .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                        .compact();

        // when & then
        assertThatThrownBy(() -> jwtProvider.parse(token))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("reason", ErrorCode.INVALID_TOKEN.getMessage());
    }

    private static Duration lifetime(IssuedToken issued) {
        Claims claims =
                Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseSignedClaims(issued.token())
                        .getPayload();
        return Duration.between(
                claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());
    }
}
