package com.novaserver.auth.repository;

import com.novaserver.auth.jwt.TokenValidityProperties;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 탈퇴 등으로 즉시 무효화해야 하는 유저를 Redis에 보관한다.
 *
 * <p>키는 {@code blocklist:{userId}}다. access 토큰은 서버가 저장하지 않아 만료 전까지 유효하므로, {@link
 * com.novaserver.auth.jwt.JwtAuthenticationFilter}가 매 요청마다 이 저장소를 확인해 막는다. TTL을 access 토큰 최대 수명으로
 * 두면 토큰이 자연 만료된 뒤 항목도 함께 사라진다.
 */
@Repository
public class TokenBlocklistRepository {

    private static final String KEY_PREFIX = "blocklist:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public TokenBlocklistRepository(
            StringRedisTemplate redisTemplate, TokenValidityProperties validity) {
        this.redisTemplate = redisTemplate;
        this.ttl = maxAccessValidity(validity);
    }

    /** 유저의 모든 access 토큰을 즉시 무효화. */
    public void block(Long userId) {
        redisTemplate.opsForValue().set(key(userId), "1", ttl);
    }

    /** 유저가 차단 체크. */
    public boolean isBlocked(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(userId)));
    }

    private Duration maxAccessValidity(TokenValidityProperties validity) {
        Duration game = validity.game().access();
        Duration web = validity.web().access();
        return game.compareTo(web) >= 0 ? game : web;
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
