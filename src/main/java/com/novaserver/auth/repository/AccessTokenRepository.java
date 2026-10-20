package com.novaserver.auth.repository;

import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.TokenValidityProperties;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 사용자별로 현재 유효한 access 토큰의 고유 ID(jti)를 Redis에 보관한다.
 *
 * <p>키는 {@code access:{clientType}:{userId}}이고, 값은 jti다. 새 로그인으로 덮어써지면 이전 access 토큰은 서명·만료가 유효해도
 * {@link com.novaserver.auth.jwt.JwtAuthenticationFilter}가 거부한다(계정당 세션 1개 강제). 저장된 값이 없으면 통과시킨다 — 이
 * 기능 배포 전에 발급된 토큰이나 로그인 흐름을 거치지 않고 만들어진 토큰까지 막을 이유는 없다.
 *
 * <p>{@link #tryAcquireLoginLock}/{@link #releaseLoginLock}은 별도 키({@code
 * login-lock:{clientType}:{userId}})로 "이미 로그인 중인지 확인 → 토큰 발급" 구간을 직렬화하는 짧은 잠금이다. 이 둘은 서로 다른 요청(두
 * 기기)이 같은 계정으로 동시에 로그인을 시도할 때를 막는 용도이고, 세션 자체의 유효성 판단은 여전히 위의 access 토큰 jti가 한다.
 */
@Repository
public class AccessTokenRepository {

    private static final String KEY_PREFIX = "access:";
    private static final String LOGIN_LOCK_KEY_PREFIX = "login-lock:";
    private static final Duration LOGIN_LOCK_TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redisTemplate;
    private final TokenValidityProperties validity;

    /**
     * Redis 템플릿과 토큰 유효기간으로 저장소를 만든다.
     *
     * @param redisTemplate Redis 접근 템플릿
     * @param validity 클라이언트 종류별 토큰 유효기간. access 유효기간을 저장 TTL로 쓴다
     */
    public AccessTokenRepository(
            StringRedisTemplate redisTemplate, TokenValidityProperties validity) {
        this.redisTemplate = redisTemplate;
        this.validity = validity;
    }

    /**
     * 사용자의 access 토큰 ID를 저장한다. 같은 사용자·클라이언트의 기존 값은 덮어쓴다. TTL은 클라이언트 종류별 access 유효기간이다.
     *
     * @param userId 토큰 주인
     * @param clientType 발급 대상 클라이언트
     * @param tokenId access 토큰의 jti
     */
    public void save(Long userId, ClientType clientType, String tokenId) {
        redisTemplate
                .opsForValue()
                .set(key(userId, clientType), tokenId, validity.of(clientType).access());
    }

    public Optional<String> findTokenId(Long userId, ClientType clientType) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId, clientType)));
    }

    public void delete(Long userId, ClientType clientType) {
        redisTemplate.delete(key(userId, clientType));
    }

    /**
     * "이미 로그인 중인지 확인 → 토큰 발급" 구간을 보호하는 짧은 잠금을 시도한다. 같은 사용자·클라이언트로 동시에 들어온 다른 요청이 이미 잠금을 쥐고 있으면
     * false를 반환한다. TTL은 안전장치일 뿐이다 — 정상 흐름에서는 {@link #releaseLoginLock}이 즉시 해제한다.
     *
     * @param userId 로그인하려는 유저
     * @param clientType 로그인 클라이언트 종류
     * @return 잠금을 획득했으면 true
     */
    public boolean tryAcquireLoginLock(Long userId, ClientType clientType) {
        Boolean acquired =
                redisTemplate
                        .opsForValue()
                        .setIfAbsent(loginLockKey(userId, clientType), "1", LOGIN_LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    /** {@link #tryAcquireLoginLock}으로 얻은 잠금을 해제한다. */
    public void releaseLoginLock(Long userId, ClientType clientType) {
        redisTemplate.delete(loginLockKey(userId, clientType));
    }

    private String key(Long userId, ClientType clientType) {
        return KEY_PREFIX + clientType.name() + ":" + userId;
    }

    private String loginLockKey(Long userId, ClientType clientType) {
        return LOGIN_LOCK_KEY_PREFIX + clientType.name() + ":" + userId;
    }
}
