package com.novaserver.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.novaserver.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class JwtProviderTest {

    private static final String SECRET =
            "test-secret-key-test-secret-key-0123456789"; // gitleaks:allow

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 900, 2592000);

    @Test
    @DisplayName("access 토큰을 파싱하면 발급 때 넣은 userId·clientType·ACCESS 타입이 나온다")
    void shouldParseAccessToken_withIssuedValues() {
        // given
        String token = jwtProvider.createAccessToken(42L, ClientType.GAME);

        // when
        TokenInfo tokenInfo = jwtProvider.parse(token);

        // then
        assertThat(tokenInfo.userId()).isEqualTo(42L);
        assertThat(tokenInfo.clientType()).isEqualTo(ClientType.GAME);
        assertThat(tokenInfo.tokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(tokenInfo.tokenId()).isNotBlank();
    }

    @Test
    @DisplayName("refresh 토큰을 파싱하면 발급 때 받은 tokenId(jti)와 같은 값이 나온다")
    void shouldParseRefreshToken_withSameTokenIdAsIssued() {
        // given
        IssuedToken issued = jwtProvider.createRefreshToken(7L, ClientType.WEB);

        // when
        TokenInfo tokenInfo = jwtProvider.parse(issued.token());

        // then
        assertThat(tokenInfo.tokenId()).isEqualTo(issued.tokenId());
        assertThat(tokenInfo.userId()).isEqualTo(7L);
        assertThat(tokenInfo.clientType()).isEqualTo(ClientType.WEB);
        assertThat(tokenInfo.tokenType()).isEqualTo(TokenType.REFRESH);
    }

    @Test
    @DisplayName("refresh 토큰은 발급할 때마다 tokenId가 다르다")
    void shouldIssueDifferentTokenId_eachTime() {
        // when
        IssuedToken first = jwtProvider.createRefreshToken(42L, ClientType.GAME);
        IssuedToken second = jwtProvider.createRefreshToken(42L, ClientType.GAME);

        // then
        assertThat(first.tokenId()).isNotEqualTo(second.tokenId());
    }

    @Test
    @DisplayName("만료된 토큰이면 TOKEN_EXPIRED 예외를 던진다")
    void shouldThrowTokenExpired_whenTokenIsExpired() {
        // given
        String token = new JwtProvider(SECRET, -60, -60).createAccessToken(42L, ClientType.GAME);

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
}
