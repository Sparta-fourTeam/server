package com.novaserver.auth.repository;

import com.novaserver.auth.jwt.ClientType;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 사용자별로 현재 유효한 refresh 토큰의 고유 ID(jti)를 Redis에 보관한다.
 *
 * <p>키는 {@code refresh:{clientType}:{userId}}이고, 값은 jti다. 토큰 문자열은 저장하지 않는다.
 */
@Repository
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    /**
     * Redis 템플릿과 refresh 토큰 유효 시간으로 저장소를 만든다.
     *
     * @param redisTemplate Redis 접근 템플릿
     * @param refreshTokenValiditySeconds refresh 토큰 유효 시간(초). 저장 TTL로 쓴다
     */
    public RefreshTokenRepository(
            StringRedisTemplate redisTemplate,
            @Value("${app.jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(refreshTokenValiditySeconds);
    }

    /**
     * 사용자의 refresh 토큰 ID를 저장한다. 같은 사용자·클라이언트의 기존 값은 덮어쓴다.
     *
     * @param userId 토큰 주인
     * @param clientType 발급 대상 클라이언트
     * @param tokenId refresh 토큰의 jti
     */
    public void save(Long userId, ClientType clientType, String tokenId) {
        redisTemplate.opsForValue().set(key(userId, clientType), tokenId, ttl);
    }

    public Optional<String> findTokenId(Long userId, ClientType clientType) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId, clientType)));
    }

    public void delete(Long userId, ClientType clientType) {
        redisTemplate.delete(key(userId, clientType));
    }

    private String key(Long userId, ClientType clientType) {
        return KEY_PREFIX + clientType.name() + ":" + userId;
    }
}
