package com.novaserver.auth.jwt;

import com.novaserver.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** JWT access/refresh 토큰을 만들고 검증한다. */
@Component
public class JwtProvider {

    private static final String TYPE_CLAIM = "type";

    private final SecretKey secretKey;
    private final JwtParser jwtParser;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;

    /**
     * 설정값으로 서명 키와 파서를 만든다.
     *
     * @param secret 서명 키 문자열, 32바이트 이상
     * @param accessTokenValiditySeconds access 토큰 유효 시간(초)
     * @param refreshTokenValiditySeconds refresh 토큰 유효 시간(초)
     */
    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-validity-seconds}") long accessTokenValiditySeconds,
            @Value("${app.jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    public String createAccessToken(Long playerId, ClientType clientType) {
        return createToken(playerId, clientType, TokenType.ACCESS, accessTokenValiditySeconds);
    }

    public String createRefreshToken(Long playerId, ClientType clientType) {
        return createToken(playerId, clientType, TokenType.REFRESH, refreshTokenValiditySeconds);
    }

    /**
     * 토큰을 검증하고 payload를 꺼낸다.
     *
     * @param token 검증할 토큰
     * @return 토큰에 담긴 플레이어 id, 클라이언트 종류, 토큰 종류
     * @throws org.springframework.web.server.ResponseStatusException 만료되면 TOKEN_EXPIRED, 위조·형식 오류면
     *     INVALID_TOKEN
     */
    public TokenInfo parse(String token) {
        Claims claims = parseClaims(token);
        return new TokenInfo(
                Long.valueOf(claims.getSubject()),
                ClientType.valueOf(claims.getAudience().iterator().next()),
                TokenType.valueOf(claims.get(TYPE_CLAIM, String.class)));
    }

    private String createToken(
            Long playerId, ClientType clientType, TokenType tokenType, long validitySeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validitySeconds * 1000);

        return Jwts.builder()
                .subject(String.valueOf(playerId))
                .audience()
                .add(clientType.name())
                .and()
                .claim(TYPE_CLAIM, tokenType.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        try {
            return jwtParser.parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw ErrorCode.TOKEN_EXPIRED.exception();
        } catch (JwtException | IllegalArgumentException e) {
            throw ErrorCode.INVALID_TOKEN.exception();
        }
    }
}
