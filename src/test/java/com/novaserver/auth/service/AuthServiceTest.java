package com.novaserver.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.novaserver.auth.dto.GuestSignUpResponse;
import com.novaserver.auth.dto.TokenResponse;
import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.GameWebSocketSessionRegistry;
import com.novaserver.auth.jwt.IssuedToken;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.jwt.TokenInfo;
import com.novaserver.auth.jwt.TokenType;
import com.novaserver.auth.repository.AccessTokenRepository;
import com.novaserver.auth.repository.RefreshTokenRepository;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.auth.social.SocialTokenVerifier;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.notification.service.SseEmitterService;
import com.novaserver.quest.service.UserQuestService;
import com.novaserver.user.Provider;
import com.novaserver.user.Role;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.service.UserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String GUEST_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final Long USER_ID = 42L;
    private static final String OLD_REFRESH = "old-refresh-token";
    private static final String ID_TOKEN = "google-id-token";
    private static final String SUB = "google-sub-1";

    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private UserQuestService quests;
    @Mock private JwtProvider jwtProvider;
    @Mock private AccessTokenRepository accessTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenBlocklistRepository tokenBlocklistRepository;
    @Mock private SocialTokenVerifier socialTokenVerifier;
    @Mock private GameWebSocketSessionRegistry gameWebSocketSessionRegistry;
    @Mock private SseEmitterService sseEmitterService;

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
        assertThat(response.getTokens().getAccessToken()).isEqualTo("access-token");
        assertThat(response.getTokens().getRefreshToken()).isEqualTo("refresh-token");
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
    @DisplayName("GAME 로그인 시 이전에 열려있던 WebSocket·SSE 연결을 끊는다")
    void shouldDisconnectExistingWebSocketSession_onGameLogin() {
        // given
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
        stubTokens();

        // when
        authService.guestSignUp();

        // then
        verify(gameWebSocketSessionRegistry).disconnectExisting(USER_ID);
        verify(sseEmitterService).disconnectExistingGameSessions(USER_ID);
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

    @Test
    @DisplayName("저장된 tokenId와 일치하면 새 토큰을 발급하고 새 tokenId로 덮어쓴다")
    void shouldRotateTokens_whenRefreshTokenMatches() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.USER, ClientType.GAME, TokenType.REFRESH, "old-jti"));
        when(refreshTokenRepository.findTokenId(USER_ID, ClientType.GAME))
                .thenReturn(Optional.of("old-jti"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(storedUser(Role.USER)));
        stubTokens();

        // when
        TokenResponse response = authService.refresh(OLD_REFRESH, ClientType.GAME);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(USER_ID, ClientType.GAME, "refresh-jti");
        verify(refreshTokenRepository, never()).delete(anyLong(), any());
    }

    @Test
    @DisplayName("GAME 토큰 재발급은 같은 세션의 연장이므로 WebSocket 연결을 끊지 않는다")
    void shouldNotDisconnectWebSocketSession_onGameRefresh() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.USER, ClientType.GAME, TokenType.REFRESH, "old-jti"));
        when(refreshTokenRepository.findTokenId(USER_ID, ClientType.GAME))
                .thenReturn(Optional.of("old-jti"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(storedUser(Role.USER)));
        stubTokens();

        // when
        authService.refresh(OLD_REFRESH, ClientType.GAME);

        // then
        verifyNoInteractions(gameWebSocketSessionRegistry, sseEmitterService);
    }

    @Test
    @DisplayName("WEB 경로에서는 WEB refresh로 WEB 토큰을 재발급한다")
    void shouldKeepClientType_onRefresh() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.USER, ClientType.WEB, TokenType.REFRESH, "old-jti"));
        when(refreshTokenRepository.findTokenId(USER_ID, ClientType.WEB))
                .thenReturn(Optional.of("old-jti"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(storedUser(Role.USER)));
        when(jwtProvider.createAccessToken(USER_ID, ClientType.WEB, Role.USER))
                .thenReturn("web-access");
        when(jwtProvider.createRefreshToken(USER_ID, ClientType.WEB, Role.USER))
                .thenReturn(new IssuedToken("web-refresh", "web-jti"));
        when(jwtProvider.parse("web-access"))
                .thenReturn(
                        new TokenInfo(
                                USER_ID,
                                Role.USER,
                                ClientType.WEB,
                                TokenType.ACCESS,
                                "web-access-jti"));

        // when
        TokenResponse response = authService.refresh(OLD_REFRESH, ClientType.WEB);

        // then
        assertThat(response.getAccessToken()).isEqualTo("web-access");
        verify(refreshTokenRepository).save(USER_ID, ClientType.WEB, "web-jti");
        verify(gameWebSocketSessionRegistry, never()).disconnectExisting(any());
        verify(sseEmitterService, never()).disconnectExistingGameSessions(any());
    }

    @Test
    @DisplayName("access 토큰으로 재발급하면 401 INVALID_TOKEN이고 저장된 refresh는 건드리지 않는다")
    void shouldThrowInvalidToken_whenAccessTokenUsed() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.USER, ClientType.GAME, TokenType.ACCESS, "jti"));

        // when & then
        assertStatus(
                () -> authService.refresh(OLD_REFRESH, ClientType.GAME),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.INVALID_TOKEN);
        verifyNoInteractions(refreshTokenRepository);
        verify(jwtProvider, never()).createRefreshToken(anyLong(), any(), any());
    }

    @Test
    @DisplayName("저장된 refresh가 없으면 401 INVALID_TOKEN이고 새 토큰을 발급하지 않는다")
    void shouldThrowInvalidToken_whenNoStoredRefresh() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.USER, ClientType.GAME, TokenType.REFRESH, "old-jti"));
        when(refreshTokenRepository.findTokenId(USER_ID, ClientType.GAME))
                .thenReturn(Optional.empty());

        // when & then
        assertStatus(
                () -> authService.refresh(OLD_REFRESH, ClientType.GAME),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.INVALID_TOKEN);
        verify(refreshTokenRepository, never()).save(anyLong(), any(), anyString());
        verify(jwtProvider, never()).createAccessToken(anyLong(), any(), any());
    }

    @Test
    @DisplayName("저장된 값과 다른 refresh가 오면 탈취로 보고 저장된 refresh를 삭제한 뒤 401을 던진다")
    void shouldDeleteStoredRefresh_whenReusedTokenDetected() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.USER, ClientType.GAME, TokenType.REFRESH, "old-jti"));
        when(refreshTokenRepository.findTokenId(USER_ID, ClientType.GAME))
                .thenReturn(Optional.of("newer-jti"));

        // when & then
        assertStatus(
                () -> authService.refresh(OLD_REFRESH, ClientType.GAME),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.INVALID_TOKEN);
        verify(refreshTokenRepository).delete(USER_ID, ClientType.GAME);
        verify(refreshTokenRepository, never()).save(anyLong(), any(), anyString());
        verify(jwtProvider, never()).createAccessToken(anyLong(), any(), any());
    }

    @Test
    @DisplayName("만료된 refresh면 parse의 TOKEN_EXPIRED를 그대로 던지고 저장소를 건드리지 않는다")
    void shouldPropagateTokenExpired_whenRefreshExpired() {
        // given
        when(jwtProvider.parse(OLD_REFRESH)).thenThrow(ErrorCode.TOKEN_EXPIRED.exception());

        // when & then
        assertStatus(
                () -> authService.refresh(OLD_REFRESH, ClientType.GAME),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.TOKEN_EXPIRED);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("소셜 로그인: 이미 가입한 계정이면 새로 만들지 않고 GAME 토큰을 발급한다")
    void shouldIssueTokens_whenSocialUserExists() {
        // given
        when(socialTokenVerifier.verify(Provider.GOOGLE, ID_TOKEN)).thenReturn(SUB);
        when(userRepository.findByProviderAndAccountId(Provider.GOOGLE, SUB))
                .thenReturn(Optional.of(withId(new User(Provider.GOOGLE, SUB))));
        stubTokens();

        // when
        TokenResponse response =
                authService.socialLogIn(ID_TOKEN, Provider.GOOGLE, ClientType.GAME);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository).save(USER_ID, ClientType.GAME, "refresh-jti");
    }

    @Test
    @DisplayName("소셜 로그인: 처음 로그인한 계정이면 provider와 sub로 유저를 만들고 토큰을 발급한다")
    void shouldCreateUser_whenSocialUserIsNew() {
        // given
        when(socialTokenVerifier.verify(Provider.APPLE, ID_TOKEN)).thenReturn(SUB);
        when(userRepository.findByProviderAndAccountId(Provider.APPLE, SUB))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
        stubTokens();

        // when
        TokenResponse response = authService.socialLogIn(ID_TOKEN, Provider.APPLE, ClientType.GAME);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getProvider()).isEqualTo(Provider.APPLE);
        assertThat(captor.getValue().getAccountId()).isEqualTo(SUB);
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("소셜 로그인: 동시 첫 로그인으로 저장이 unique 위반이면 먼저 만들어진 유저로 토큰을 발급한다")
    void shouldUseExistingUser_whenConcurrentSignUpConflicts() {
        // given
        when(socialTokenVerifier.verify(Provider.GOOGLE, ID_TOKEN)).thenReturn(SUB);
        when(userRepository.findByProviderAndAccountId(Provider.GOOGLE, SUB))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(withId(new User(Provider.GOOGLE, SUB))));
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));
        stubTokens();

        // when
        TokenResponse response =
                authService.socialLogIn(ID_TOKEN, Provider.GOOGLE, ClientType.GAME);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(userRepository, times(2)).findByProviderAndAccountId(Provider.GOOGLE, SUB);
    }

    @Test
    @DisplayName("소셜 로그인: 저장 실패 후 다시 조회해도 없으면 원래 예외를 그대로 던진다")
    void shouldRethrowOriginalException_whenUserStillMissingAfterConflict() {
        // given
        DataIntegrityViolationException original =
                new DataIntegrityViolationException("Data too long for column");
        when(socialTokenVerifier.verify(Provider.GOOGLE, ID_TOKEN)).thenReturn(SUB);
        when(userRepository.findByProviderAndAccountId(Provider.GOOGLE, SUB))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenThrow(original);

        // when & then
        assertThatThrownBy(
                        () -> authService.socialLogIn(ID_TOKEN, Provider.GOOGLE, ClientType.GAME))
                .isSameAs(original);
        verifyNoInteractions(jwtProvider, refreshTokenRepository);
    }

    @Test
    @DisplayName("소셜 로그인: ID 토큰 검증에 실패하면 예외를 그대로 던지고 유저를 조회·생성하지 않는다")
    void shouldNotTouchUsers_whenIdTokenInvalid() {
        // given
        when(socialTokenVerifier.verify(Provider.GOOGLE, ID_TOKEN))
                .thenThrow(ErrorCode.INVALID_SOCIAL_TOKEN.exception());

        // when & then
        assertStatus(
                () -> authService.socialLogIn(ID_TOKEN, Provider.GOOGLE, ClientType.GAME),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.INVALID_SOCIAL_TOKEN);
        verifyNoInteractions(userRepository, jwtProvider, refreshTokenRepository);
    }

    @Test
    @DisplayName("소셜 연동: 게스트 계정에 Google 계정을 연동하면 provider·accountId가 바뀌고 flush로 즉시 반영한다")
    void shouldLinkGoogleAccount_whenGuestLinks() {
        // given
        User user = storedUser(Role.USER);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(socialTokenVerifier.verify(Provider.GOOGLE, ID_TOKEN)).thenReturn(SUB);
        when(userRepository.findByProviderAndAccountId(Provider.GOOGLE, SUB))
                .thenReturn(Optional.empty());

        // when
        authService.linkSocialAccount(USER_ID, ID_TOKEN, Provider.GOOGLE);

        // then
        assertThat(user.getProvider()).isEqualTo(Provider.GOOGLE);
        assertThat(user.getAccountId()).isEqualTo(SUB);
        verify(userRepository).flush();
    }

    @Test
    @DisplayName("소셜 연동: 로그인하지 않았으면 401 UNAUTHORIZED이고 아무것도 조회하지 않는다")
    void shouldThrowUnauthorized_whenLinkingWithoutLogin() {
        // when & then
        assertStatus(
                () -> authService.linkSocialAccount(null, ID_TOKEN, Provider.GOOGLE),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED);
        verifyNoInteractions(userRepository, socialTokenVerifier);
    }

    @Test
    @DisplayName("소셜 연동: 유저가 없으면 404 USER_DATA_NOT_FOUND이고 소셜 토큰은 검증하지 않는다")
    void shouldThrowNotFound_whenLinkingUserMissing() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when & then
        assertStatus(
                () -> authService.linkSocialAccount(USER_ID, ID_TOKEN, Provider.GOOGLE),
                HttpStatus.NOT_FOUND,
                ErrorCode.USER_DATA_NOT_FOUND,
                USER_ID);
        verifyNoInteractions(socialTokenVerifier);
    }

    @Test
    @DisplayName("소셜 연동: 다른 소셜 계정이 이미 연동된 유저면 409 ALREADY_LINKED_PROVIDER를 던진다")
    void shouldThrowConflict_whenUserAlreadyLinkedToProvider() {
        // given
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(withId(new User(Provider.GOOGLE, "already-linked-sub"))));
        when(socialTokenVerifier.verify(Provider.GOOGLE, ID_TOKEN)).thenReturn(SUB);

        // when & then
        assertStatus(
                () -> authService.linkSocialAccount(USER_ID, ID_TOKEN, Provider.GOOGLE),
                HttpStatus.CONFLICT,
                ErrorCode.ALREADY_LINKED_PROVIDER);
    }

    @Test
    @DisplayName("소셜 연동: 이미 연동된 것과 같은 계정으로 재시도하면 예외 없이 성공 처리한다")
    void shouldSucceed_whenRetryingSameLinkedAccount() {
        // given
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(withId(new User(Provider.GOOGLE, SUB))));
        when(socialTokenVerifier.verify(Provider.GOOGLE, ID_TOKEN)).thenReturn(SUB);

        // when
        authService.linkSocialAccount(USER_ID, ID_TOKEN, Provider.GOOGLE);

        // then
        verify(userRepository, never()).findByProviderAndAccountId(any(), any());
        verify(userRepository, never()).flush();
    }

    @Test
    @DisplayName("소셜 연동: 연동하려는 Google 계정이 이미 다른 유저에 연동되어 있으면 409 SOCIAL_ACCOUNT_ALREADY_LINKED")
    void shouldThrowConflict_whenRequestedAccountAlreadyLinkedToAnotherUser() {
        // given
        User user = storedUser(Role.USER);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(socialTokenVerifier.verify(Provider.GOOGLE, ID_TOKEN)).thenReturn(SUB);
        when(userRepository.findByProviderAndAccountId(Provider.GOOGLE, SUB))
                .thenReturn(Optional.of(withId(new User(Provider.GOOGLE, SUB))));

        // when & then
        assertStatus(
                () -> authService.linkSocialAccount(USER_ID, ID_TOKEN, Provider.GOOGLE),
                HttpStatus.CONFLICT,
                ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
        assertThat(user.getProvider()).isEqualTo(Provider.GUEST);
        verify(userRepository, never()).flush();
    }

    @Test
    @DisplayName("소셜 연동: 같은 Google 계정의 동시 연동이 unique 위반으로 충돌하면 409 SOCIAL_ACCOUNT_ALREADY_LINKED")
    void shouldThrowConflict_whenConcurrentLinkingConflicts() {
        // given
        User user = storedUser(Role.USER);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(socialTokenVerifier.verify(Provider.GOOGLE, ID_TOKEN)).thenReturn(SUB);
        when(userRepository.findByProviderAndAccountId(Provider.GOOGLE, SUB))
                .thenReturn(Optional.empty());
        doThrow(new DataIntegrityViolationException("Duplicate entry"))
                .when(userRepository)
                .flush();

        // when & then
        assertStatus(
                () -> authService.linkSocialAccount(USER_ID, ID_TOKEN, Provider.GOOGLE),
                HttpStatus.CONFLICT,
                ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
    }

    @Test
    @DisplayName("경로의 clientType과 토큰의 clientType이 다르면 401이고 저장된 refresh는 건드리지 않는다")
    void shouldThrowInvalidToken_whenClientTypeMismatches() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.USER, ClientType.GAME, TokenType.REFRESH, "old-jti"));

        // when & then
        assertStatus(
                () -> authService.refresh(OLD_REFRESH, ClientType.WEB),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.INVALID_TOKEN);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("재발급하면 토큰의 role이 아니라 DB의 role로 새 토큰을 만든다")
    void shouldUseRoleFromDatabase_onRefresh() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.USER, ClientType.WEB, TokenType.REFRESH, "old-jti"));
        when(refreshTokenRepository.findTokenId(USER_ID, ClientType.WEB))
                .thenReturn(Optional.of("old-jti"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(storedUser(Role.ADMIN)));
        when(jwtProvider.createAccessToken(USER_ID, ClientType.WEB, Role.ADMIN))
                .thenReturn("admin-access");
        when(jwtProvider.createRefreshToken(USER_ID, ClientType.WEB, Role.ADMIN))
                .thenReturn(new IssuedToken("admin-refresh", "admin-jti"));
        when(jwtProvider.parse("admin-access"))
                .thenReturn(
                        new TokenInfo(
                                USER_ID,
                                Role.ADMIN,
                                ClientType.WEB,
                                TokenType.ACCESS,
                                "admin-access-jti"));

        // when
        TokenResponse response = authService.refresh(OLD_REFRESH, ClientType.WEB);

        // then
        assertThat(response.getAccessToken()).isEqualTo("admin-access");
    }

    @Test
    @DisplayName("refresh는 유효하지만 유저가 없으면 401 INVALID_TOKEN이고 토큰을 발급하지 않는다")
    void shouldThrowInvalidToken_whenUserDeleted() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.USER, ClientType.GAME, TokenType.REFRESH, "old-jti"));
        when(refreshTokenRepository.findTokenId(USER_ID, ClientType.GAME))
                .thenReturn(Optional.of("old-jti"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when & then
        assertStatus(
                () -> authService.refresh(OLD_REFRESH, ClientType.GAME),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.INVALID_TOKEN);
        verify(jwtProvider, never()).createAccessToken(anyLong(), any(), any());
    }

    @Test
    @DisplayName("refresh 토큰이 없으면(쿠키 없음) 401 INVALID_TOKEN")
    void shouldThrowInvalidToken_whenRefreshTokenMissing() {
        assertStatus(
                () -> authService.refresh(null, ClientType.WEB),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.INVALID_TOKEN);
        verifyNoInteractions(jwtProvider, refreshTokenRepository);
    }

    @Test
    @DisplayName("게임 로그아웃: 저장된 값과 같은 refresh면 Redis에서 지운다")
    void shouldDeleteStoredRefresh_onGameLogOut() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.USER, ClientType.GAME, TokenType.REFRESH, "old-jti"));
        when(refreshTokenRepository.findTokenId(USER_ID, ClientType.GAME))
                .thenReturn(Optional.of("old-jti"));

        // when
        authService.logOut(OLD_REFRESH, ClientType.GAME);

        // then
        verify(refreshTokenRepository).delete(USER_ID, ClientType.GAME);
    }

    @Test
    @DisplayName("게임 로그아웃: WEB 토큰으로 요청하면 지우지 않는다")
    void shouldDoNothing_whenGameLogOutWithWebToken() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.USER, ClientType.WEB, TokenType.REFRESH, "old-jti"));

        // when
        authService.logOut(OLD_REFRESH, ClientType.GAME);

        // then
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("Web 로그아웃: 저장된 값과 같은 refresh면 Redis에서 지운다")
    void shouldDeleteStoredRefresh_onWebLogOut() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.ADMIN, ClientType.WEB, TokenType.REFRESH, "old-jti"));
        when(refreshTokenRepository.findTokenId(USER_ID, ClientType.WEB))
                .thenReturn(Optional.of("old-jti"));

        // when
        authService.webLogOut(OLD_REFRESH);

        // then
        verify(refreshTokenRepository).delete(USER_ID, ClientType.WEB);
    }

    @Test
    @DisplayName("Web 로그아웃: 예전 refresh면 현재 세션을 지우지 않는다")
    void shouldKeepCurrentSession_whenLogOutWithOldRefresh() {
        // given
        when(jwtProvider.parse(OLD_REFRESH))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.ADMIN, ClientType.WEB, TokenType.REFRESH, "old-jti"));
        when(refreshTokenRepository.findTokenId(USER_ID, ClientType.WEB))
                .thenReturn(Optional.of("newer-jti"));

        // when
        authService.webLogOut(OLD_REFRESH);

        // then
        verify(refreshTokenRepository, never()).delete(anyLong(), any());
    }

    @Test
    @DisplayName("Web 로그아웃: 토큰이 없거나 만료됐거나 GAME 토큰이면 예외 없이 아무것도 지우지 않는다")
    void shouldDoNothing_whenLogOutWithoutValidWebRefresh() {
        // given
        when(jwtProvider.parse("expired")).thenThrow(ErrorCode.TOKEN_EXPIRED.exception());
        when(jwtProvider.parse("game"))
                .thenReturn(
                        new TokenInfo(
                                USER_ID, Role.USER, ClientType.GAME, TokenType.REFRESH, "jti"));

        // when
        authService.webLogOut(null);
        authService.webLogOut("expired");
        authService.webLogOut("game");

        // then
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName(
            "탈퇴: 유저 데이터를 삭제하고 GAME·WEB의 refresh·access 세션을 지운 뒤 블록리스트에 등록하고 WebSocket·SSE 연결도 끊는다")
    void shouldDeleteDataAndRevokeTokens_onDeleteUser() {
        // when
        authService.deleteUser(USER_ID);

        // then
        verify(userService).deleteAccount(USER_ID);
        verify(refreshTokenRepository).delete(USER_ID, ClientType.GAME);
        verify(refreshTokenRepository).delete(USER_ID, ClientType.WEB);
        verify(accessTokenRepository).delete(USER_ID, ClientType.GAME);
        verify(accessTokenRepository).delete(USER_ID, ClientType.WEB);
        verify(tokenBlocklistRepository).block(USER_ID);
        verify(gameWebSocketSessionRegistry).disconnectExisting(USER_ID);
        verify(sseEmitterService).disconnectExistingGameSessions(USER_ID);
    }

    @Test
    @DisplayName("탈퇴: 로그인하지 않았으면 401 UNAUTHORIZED이고 아무것도 지우지 않는다")
    void shouldThrowUnauthorized_whenDeletingWithoutLogin() {
        // when & then
        assertStatus(
                () -> authService.deleteUser(null),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED);
        verifyNoInteractions(
                userService,
                refreshTokenRepository,
                tokenBlocklistRepository,
                gameWebSocketSessionRegistry,
                sseEmitterService);
    }

    private static void assertStatus(
            ThrowingCallable call, HttpStatus status, ErrorCode errorCode, Object... args) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e -> {
                            ResponseStatusException ex = (ResponseStatusException) e;
                            assertThat(ex.getStatusCode()).isEqualTo(status);
                            assertThat(ex.getReason())
                                    .isEqualTo(errorCode.getMessage().formatted(args));
                        });
    }

    private void stubTokens() {
        when(jwtProvider.createAccessToken(USER_ID, ClientType.GAME, Role.USER))
                .thenReturn("access-token");
        when(jwtProvider.createRefreshToken(USER_ID, ClientType.GAME, Role.USER))
                .thenReturn(new IssuedToken("refresh-token", "refresh-jti"));
        when(jwtProvider.parse("access-token"))
                .thenReturn(
                        new TokenInfo(
                                USER_ID,
                                Role.USER,
                                ClientType.GAME,
                                TokenType.ACCESS,
                                "access-jti"));
    }

    private static User storedUser(Role role) {
        User user = withId(new User(Provider.GUEST, "hashed-guest-id"));
        ReflectionTestUtils.setField(user, "role", role);
        return user;
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
