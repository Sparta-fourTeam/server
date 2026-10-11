package com.novaserver.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.novaserver.auth.dto.GuestSignUpResponse;
import com.novaserver.auth.dto.TokenResponse;
import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.IssuedToken;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.repository.RefreshTokenRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.Provider;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String GUEST_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final Long USER_ID = 42L;

    @Mock private UserRepository userRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private AuthService authService;

    @Test
    @DisplayName("게스트 가입 시 DB에는 guestId의 SHA-256 해시를 저장하고 응답에는 원본 guestId를 준다")
    void shouldSaveHashedGuestId_andReturnRawGuestId_onSignUp() {
        // given
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
        stubTokens();

        // when
        GuestSignUpResponse response = authService.guestSignUp();

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(UUID.fromString(response.getGuestId()).toString())
                .isEqualTo(response.getGuestId());
        assertThat(saved.getProvider()).isEqualTo(Provider.GUEST);
        assertThat(saved.getAccountId())
                .isEqualTo(sha256(response.getGuestId()))
                .isNotEqualTo(response.getGuestId())
                .hasSize(64);
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("게스트 가입 시 refresh 토큰의 tokenId를 저장한다")
    void shouldSaveRefreshTokenId_onSignUp() {
        // given
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
        stubTokens();

        // when
        authService.guestSignUp();

        // then
        verify(refreshTokenRepository).save(USER_ID, ClientType.GAME, "refresh-jti");
    }

    @Test
    @DisplayName("등록된 guestId로 로그인하면 해시로 조회해 토큰을 발급하고 refresh tokenId를 저장한다")
    void shouldIssueTokens_whenGuestExists() {
        // given
        when(userRepository.findByProviderAndAccountId(Provider.GUEST, sha256(GUEST_ID)))
                .thenReturn(Optional.of(withId(new User(Provider.GUEST, sha256(GUEST_ID)))));
        stubTokens();

        // when
        TokenResponse response = authService.guestLogIn(GUEST_ID);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(USER_ID, ClientType.GAME, "refresh-jti");
    }

    @Test
    @DisplayName("대문자로 보낸 guestId도 소문자와 같은 해시로 조회한다")
    void shouldNormalizeGuestId_beforeHashing() {
        // given
        when(userRepository.findByProviderAndAccountId(Provider.GUEST, sha256(GUEST_ID)))
                .thenReturn(Optional.of(withId(new User(Provider.GUEST, sha256(GUEST_ID)))));
        stubTokens();

        // when
        TokenResponse response = authService.guestLogIn(GUEST_ID.toUpperCase());

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("UUID 형식이 아니면 400 INVALID_REQUEST를 던지고 조회하지 않는다")
    void shouldThrowBadRequest_whenGuestIdIsNotUuid() {
        // when & then
        assertThatThrownBy(() -> authService.guestLogIn("not-a-uuid"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e -> {
                            ResponseStatusException ex = (ResponseStatusException) e;
                            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                            assertThat(ex.getReason())
                                    .isEqualTo(ErrorCode.INVALID_REQUEST.getMessage());
                        });
        verifyNoInteractions(userRepository, jwtProvider, refreshTokenRepository);
    }

    @Test
    @DisplayName("일치하는 게스트가 없으면 401 INVALID_GUEST_ID를 던지고 새로 만들거나 토큰을 발급하지 않는다")
    void shouldThrowUnauthorized_whenGuestNotFound() {
        // given
        when(userRepository.findByProviderAndAccountId(Provider.GUEST, sha256(GUEST_ID)))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.guestLogIn(GUEST_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e -> {
                            ResponseStatusException ex = (ResponseStatusException) e;
                            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                            assertThat(ex.getReason())
                                    .isEqualTo(ErrorCode.INVALID_GUEST_ID.getMessage());
                        });
        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtProvider, refreshTokenRepository);
    }

    private void stubTokens() {
        when(jwtProvider.createAccessToken(USER_ID, ClientType.GAME)).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(USER_ID, ClientType.GAME))
                .thenReturn(new IssuedToken("refresh-token", "refresh-jti"));
    }

    private static User withId(User user) {
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private static String sha256(String raw) {
        try {
            byte[] bytes =
                    MessageDigest.getInstance("SHA-256")
                            .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
