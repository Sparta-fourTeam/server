package com.novaserver.auth.jwt;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** JWT access/refresh 토큰을 만들고 검증한다. */
@Component
@EnableConfigurationProperties(TokenValidityProperties.class)
public class JwtProvider {

    private static final String TYPE_CLAIM = "type";
    private static final String ROLE_CLAIM = "role";

    private final SecretKey secretKey;
    private final JwtParser jwtParser;
    private final TokenValidityProperties validity;

    /**
     * 설정값으로 서명 키와 파서를 만든다.
     *
     * @param secret 서명 키 문자열, 32바이트 이상
     * @param validity 클라이언트 종류별 토큰 유효기간
     */
    public JwtProvider(
            @Value("${app.jwt.secret}") String secret, TokenValidityProperties validity) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
        this.validity = validity;
    }

    /**
     * 새 access 토큰을 만든다. 유효기간은 클라이언트 종류에 따라 정해진다.
     *
     * @param userId 토큰 주인
     * @param clientType 발급 대상 클라이언트
     * @param role 토큰 주인의 권한
     * @return access 토큰 문자열
     */
    public String createAccessToken(Long userId, ClientType clientType, Role role) {
        return createToken(
                        userId,
                        clientType,
                        TokenType.ACCESS,
                        role,
                        validity.of(clientType).access())
                .token();
    }

    /**
     * 새 refresh 토큰을 만든다. 반환된 tokenId는 refresh 저장소에 저장해야 재발급 때 쓸 수 있다.
     *
     * @param userId 토큰 주인
     * @param clientType 발급 대상 클라이언트
     * @param role 토큰 주인의 권한
     * @return 토큰 문자열과 고유 ID
     */
    public IssuedToken createRefreshToken(Long userId, ClientType clientType, Role role) {
        return createToken(
                userId, clientType, TokenType.REFRESH, role, validity.of(clientType).refresh());
    }

    /**
     * 토큰을 검증하고 payload를 꺼낸다.
     *
     * @param token 검증할 토큰
     * @return 토큰에 담긴 사용자 id, 권한, 클라이언트 종류, 토큰 종류, 고유 ID
     * @throws org.springframework.web.server.ResponseStatusException 만료되면 TOKEN_EXPIRED, 위조·형식
     *     오류이거나 role 클레임이 없으면(role 도입 전에 발급된 토큰) INVALID_TOKEN
     */
    public TokenInfo parse(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get(ROLE_CLAIM, String.class);
        if (role == null) {
            throw ErrorCode.INVALID_TOKEN.exception();
        }
        return new TokenInfo(
                Long.valueOf(claims.getSubject()),
                Role.valueOf(role),
                ClientType.valueOf(claims.getAudience().iterator().next()),
                TokenType.valueOf(claims.get(TYPE_CLAIM, String.class)),
                claims.getId());
    }

    private IssuedToken createToken(
            Long userId, ClientType clientType, TokenType tokenType, Role role, Duration validity) {
        String tokenId = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validity.toMillis());

        String token =
                Jwts.builder()
                        .id(tokenId)
                        .subject(String.valueOf(userId))
                        .audience()
                        .add(clientType.name())
                        .and()
                        .claim(TYPE_CLAIM, tokenType.name())
                        .claim(ROLE_CLAIM, role.name())
                        .issuedAt(now)
                        .expiration(expiry)
                        .signWith(secretKey)
                        .compact();
        return new IssuedToken(token, tokenId);
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
