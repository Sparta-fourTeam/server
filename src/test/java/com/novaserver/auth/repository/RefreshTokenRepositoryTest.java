package com.novaserver.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.auth.jwt.ClientType;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryTest {

    private static final long REFRESH_VALIDITY_SECONDS = 2592000;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository =
                new RefreshTokenRepository(redisTemplate, REFRESH_VALIDITY_SECONDS);
    }

    @Test
    @DisplayName("저장하면 refresh:{clientType}:{userId} 키에 jti를 refresh 유효기간 TTL로 넣는다")
    void shouldSaveTokenId_withKeyAndTtl() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        refreshTokenRepository.save(42L, ClientType.GAME, "jti-1");

        // then
        verify(valueOperations)
                .set("refresh:GAME:42", "jti-1", Duration.ofSeconds(REFRESH_VALIDITY_SECONDS));
    }

    @Test
    @DisplayName("저장된 값이 있으면 jti를 반환한다")
    void shouldReturnTokenId_whenStored() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:WEB:7")).thenReturn("jti-1");

        // when
        Optional<String> tokenId = refreshTokenRepository.findTokenId(7L, ClientType.WEB);

        // then
        assertThat(tokenId).contains("jti-1");
    }

    @Test
    @DisplayName("저장된 값이 없으면 빈 Optional을 반환한다")
    void shouldReturnEmpty_whenNotStored() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:GAME:42")).thenReturn(null);

        // when
        Optional<String> tokenId = refreshTokenRepository.findTokenId(42L, ClientType.GAME);

        // then
        assertThat(tokenId).isEmpty();
    }

    @Test
    @DisplayName("삭제하면 해당 키를 지운다")
    void shouldDeleteKey() {
        // when
        refreshTokenRepository.delete(42L, ClientType.GAME);

        // then
        verify(redisTemplate).delete("refresh:GAME:42");
    }
}
