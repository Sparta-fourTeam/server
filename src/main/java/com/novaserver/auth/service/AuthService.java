package com.novaserver.auth.service;

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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 게스트·소셜 로그인과 토큰 발급·재발급을 담당한다. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserQuestService quests;
    private final JwtProvider jwtProvider;
    private final AccessTokenRepository accessTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlocklistRepository tokenBlocklistRepository;
    private final SocialTokenVerifier socialTokenVerifier;
    private final GameWebSocketSessionRegistry gameWebSocketSessionRegistry;
    private final SseEmitterService sseEmitterService;

    /**
     * 새 게스트 유저를 만들고 토큰을 발급한다. DB에는 guestId의 해시만 저장한다.
     *
     * @return 원본 guestId와 access·refresh 토큰
     */
    @Transactional
    public GuestSignUpResponse guestSignUp() {
        String guestId = UUID.randomUUID().toString();
        User user = userRepository.save(new User(Provider.GUEST, hash(guestId)));
        userService.createInitialState(user);
        return new GuestSignUpResponse(
                guestId, issueTokensForNewLogin(user.getId(), ClientType.GAME, Role.USER));
    }

    /**
     * 게스트 ID로 게스트를 찾아 토큰을 발급한다. 없는 게스트면 새로 만들지 않는다.
     *
     * @param guestId 가입 때 받은 게스트 ID
     * @return access·refresh 토큰
     * @throws org.springframework.web.server.ResponseStatusException UUID 형식이 아니면 INVALID_REQUEST,
     *     일치하는 게스트가 없으면 INVALID_GUEST_ID
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TokenResponse guestLogIn(String guestId) {
        String hashed = hash(parseUuid(guestId).toString());
        User user =
                userRepository
                        .findByProviderAndAccountId(Provider.GUEST, hashed)
                        .orElseThrow(ErrorCode.INVALID_GUEST_ID::exception);
        return issueTokensForNewLogin(user.getId(), ClientType.GAME, Role.USER);
    }

    /**
     * Refresh 토큰을 검증하고 새 access·refresh 토큰을 발급한다. 기존 refresh는 새 값으로 덮어써져 무효가 된다.
     *
     * <p>저장된 값과 다른 refresh가 오면 탈취로 보고 저장된 refresh를 삭제한다. 이후 해당 유저는 다시 로그인해야 한다. 새 토큰의 권한은 이전 토큰에서
     * 복사하지 않고 DB에서 다시 읽어, 권한 변경이 다음 재발급부터 반영되게 한다.
     *
     * @param refreshToken 클라이언트가 보관하던 refresh 토큰
     * @param clientType 이 경로가 받는 클라이언트 종류. 토큰의 clientType과 같아야 한다
     * @return 새 access·refresh 토큰
     * @throws org.springframework.web.server.ResponseStatusException 만료되면 TOKEN_EXPIRED, 토큰이 없거나
     *     refresh 토큰이 아니거나 clientType이 다르거나 저장된 값이 없거나 일치하지 않거나 유저가 없으면 INVALID_TOKEN
     */
    public TokenResponse refresh(String refreshToken, ClientType clientType) {
        if (refreshToken == null) {
            throw ErrorCode.INVALID_TOKEN.exception();
        }

        TokenInfo parsed = jwtProvider.parse(refreshToken);
        if (parsed.tokenType() != TokenType.REFRESH || parsed.clientType() != clientType) {
            throw ErrorCode.INVALID_TOKEN.exception();
        }

        String storedTokenId =
                refreshTokenRepository
                        .findTokenId(parsed.userId(), parsed.clientType())
                        .orElseThrow(ErrorCode.INVALID_TOKEN::exception);
        if (!storedTokenId.equals(parsed.tokenId())) {
            refreshTokenRepository.delete(parsed.userId(), parsed.clientType());
            throw ErrorCode.INVALID_TOKEN.exception();
        }

        User user =
                userRepository
                        .findById(parsed.userId())
                        .orElseThrow(ErrorCode.INVALID_TOKEN::exception);
        return issueTokens(user.getId(), parsed.clientType(), user.getRole());
    }

    private UUID parseUuid(String guestId) {
        try {
            return UUID.fromString(guestId);
        } catch (IllegalArgumentException e) {
            throw ErrorCode.INVALID_REQUEST.exception();
        }
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다", e);
        }
    }

    /**
     * 새 로그인에서만 쓴다. refresh 재발급은 같은 세션의 연장이므로 기존 WebSocket·SSE 연결을 끊지 않는다.
     *
     * @see #issueTokens
     */
    private TokenResponse issueTokensForNewLogin(Long userId, ClientType clientType, Role role) {
        TokenResponse tokens = issueTokens(userId, clientType, role);
        if (clientType == ClientType.GAME) {
            gameWebSocketSessionRegistry.disconnectExisting(userId);
            sseEmitterService.disconnectExistingGameSessions(userId);
        }
        return tokens;
    }

    private TokenResponse issueTokens(Long userId, ClientType clientType, Role role) {
        if (clientType == ClientType.GAME) {
            quests.recordLogin(userId);
        }
        String access = jwtProvider.createAccessToken(userId, clientType, role);
        IssuedToken refresh = jwtProvider.createRefreshToken(userId, clientType, role);
        accessTokenRepository.save(userId, clientType, jwtProvider.parse(access).tokenId());
        refreshTokenRepository.save(userId, clientType, refresh.tokenId());
        return new TokenResponse(access, refresh.token());
    }

    /**
     * Google·Apple ID 토큰을 검증하고 토큰을 발급한다. 처음 로그인한 계정이면 유저를 새로 만든다.
     *
     * <p>DB 쓰기가 유저 생성 한 번뿐이고 공개키 조회로 오래 걸릴 수 있어 트랜잭션으로 묶지 않는다. 같은 계정의 첫 로그인이 동시에 들어와도 한쪽이 먼저 만든 유저를
     * 다시 조회해 둘 다 성공한다.
     *
     * <p>기존 세션 유무를 확인하지 않는 버전이다. 호출하는 쪽이 이미 로그인 중이어도 끊고 들어갈지 판단을 끝낸 경우({@code /social/link}용 조회,
     * {@code /web/login})에 쓴다.
     *
     * @param idToken 클라이언트가 Google·Apple에서 받은 ID 토큰 원문
     * @param provider ID 토큰을 발급한 소셜 provider
     * @param clientType 발급할 토큰의 클라이언트 종류. 엔드포인트(컨트롤러)가 정한다
     * @return access·refresh 토큰
     * @throws org.springframework.web.server.ResponseStatusException 소셜 provider가 아니면
     *     UNSUPPORTED_PROVIDER, ID 토큰 검증에 실패하면 INVALID_SOCIAL_TOKEN, 공개키를 받아오지 못하면
     *     SOCIAL_PROVIDER_UNAVAILABLE
     */
    public TokenResponse socialLogIn(String idToken, Provider provider, ClientType clientType) {
        return socialLogIn(idToken, provider, clientType, true);
    }

    /**
     * Google·Apple ID 토큰을 검증하고 토큰을 발급한다. 처음 로그인한 계정이면 유저를 새로 만든다.
     *
     * <p>{@code forceLogin}이 false인데 이미 그 clientType으로 로그인 중인 기기가 있으면(= {@link
     * AccessTokenRepository}에 현재 access 토큰이 남아있으면) 끊지 않고 ALREADY_LOGGED_IN을 던진다. 클라이언트는 이 응답을 보고
     * 사용자에게 "다른 기기에서 로그인 중입니다. 강제로 로그인할까요?"를 물어본 뒤, 동의하면 {@code forceLogin=true}로 다시 요청한다.
     *
     * <p>이 확인과 토큰 발급은 {@link AccessTokenRepository#tryAcquireLoginLock}으로 직렬화한다. 같은 계정으로 두 기기가
     * {@code forceLogin=false}로 동시에 요청하면, 잠금을 먼저 얻은 쪽만 "로그인 중 아님"을 확인하고 토큰을 발급하며 나머지는 즉시
     * ALREADY_LOGGED_IN을 받는다 — 잠금 없이 둘 다 먼저 상태를 읽고 나중에 쓰면, 확인·발급 사이의 틈에 둘 다 "로그인 중 아님"으로 보고 통과해버릴 수
     * 있다.
     *
     * @param idToken 클라이언트가 Google·Apple에서 받은 ID 토큰 원문
     * @param provider ID 토큰을 발급한 소셜 provider
     * @param clientType 발급할 토큰의 클라이언트 종류. 엔드포인트(컨트롤러)가 정한다
     * @param forceLogin true면 기존 세션을 바로 끊고 로그인한다
     * @return access·refresh 토큰
     * @throws org.springframework.web.server.ResponseStatusException 소셜 provider가 아니면
     *     UNSUPPORTED_PROVIDER, ID 토큰 검증에 실패하면 INVALID_SOCIAL_TOKEN, 공개키를 받아오지 못하면
     *     SOCIAL_PROVIDER_UNAVAILABLE, forceLogin이 false인데 이미 로그인 중이거나 동시에 로그인 중이면
     *     ALREADY_LOGGED_IN
     */
    public TokenResponse socialLogIn(
            String idToken, Provider provider, ClientType clientType, boolean forceLogin) {
        String sub = socialTokenVerifier.verify(provider, idToken);
        User user =
                userRepository
                        .findByProviderAndAccountId(provider, sub)
                        .orElseGet(() -> createSocialUser(provider, sub));
        if (forceLogin) {
            return issueTokensForNewLogin(user.getId(), clientType, user.getRole());
        }
        return logInWithoutDisturbingExistingSession(user, clientType);
    }

    private TokenResponse logInWithoutDisturbingExistingSession(User user, ClientType clientType) {
        if (!accessTokenRepository.tryAcquireLoginLock(user.getId(), clientType)) {
            throw ErrorCode.ALREADY_LOGGED_IN.exception();
        }
        try {
            if (accessTokenRepository.findTokenId(user.getId(), clientType).isPresent()) {
                throw ErrorCode.ALREADY_LOGGED_IN.exception();
            }
            return issueTokensForNewLogin(user.getId(), clientType, user.getRole());
        } finally {
            accessTokenRepository.releaseLoginLock(user.getId(), clientType);
        }
    }

    private User createSocialUser(Provider provider, String sub) {
        User user;
        try {
            user = userRepository.save(new User(provider, sub));
        } catch (DataIntegrityViolationException e) {
            // 같은 계정의 첫 로그인이 동시에 들어와 다른 요청이 먼저 가입시킨 경우. 아니면 원래 예외를 그대로 던진다
            return userRepository.findByProviderAndAccountId(provider, sub).orElseThrow(() -> e);
        }
        userService.createInitialState(user);
        return user;
    }

    /**
     * 게스트 계정에 소셜 계정을 연동한다. 이미 소셜 계정이 연동된 유저거나, 연동하려는 소셜 계정이 이미 다른 유저에 연동되어 있으면 예외를 던진다.
     *
     * @param userId 연동할 게스트 유저 ID
     * @param idToken 연동할 Google·Apple ID 토큰 원문
     * @param provider ID 토큰을 발급한 소셜 provider
     * @throws org.springframework.web.server.ResponseStatusException 유저가 없으면 USER_DATA_NOT_FOUND,
     *     다른 소셜 계정이 이미 연동되어 있으면 ALREADY_LINKED_PROVIDER, 연동하려는 소셜 계정이 이미 다른 유저에 연동되어 있으면
     *     SOCIAL_ACCOUNT_ALREADY_LINKED
     */
    @Transactional
    public void linkSocialAccount(Long userId, String idToken, Provider provider) {
        if (userId == null) {
            throw ErrorCode.UNAUTHORIZED.exception();
        }
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> ErrorCode.USER_DATA_NOT_FOUND.exception(userId));

        String sub = socialTokenVerifier.verify(provider, idToken);
        if (user.getProvider() == provider && user.getAccountId().equals(sub)) {
            return;
        }
        if (user.getProvider() != Provider.GUEST) {
            throw ErrorCode.ALREADY_LINKED_PROVIDER.exception();
        }
        if (userRepository.findByProviderAndAccountId(provider, sub).isPresent()) {
            throw ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED.exception();
        }

        user.linkSocialAccount(provider, sub);
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED.exception();
        }
    }

    /**
     * Web refresh 토큰을 무효로 만든다.
     *
     * @param refreshToken 쿠키에서 꺼낸 refresh 토큰. 없으면 null
     */
    public void webLogOut(String refreshToken) {
        logOut(refreshToken, ClientType.WEB);
    }

    /**
     * Refresh 토큰을 무효로 만든다. 저장된 값과 같은 refresh일 때만 Redis에서 지운다.
     *
     * <p>로그아웃은 실패할 이유가 없는 요청이라 예외를 던지지 않는다. 토큰이 없거나 만료됐거나 clientType이 다르거나 잘못된 토큰이면 지울 것이 없으므로 그냥
     * 끝낸다. access 토큰은 서버가 저장하지 않으므로 남은 수명 동안 유효하다.
     *
     * @param refreshToken 클라이언트가 보관하던 refresh 토큰. 없으면 null
     * @param clientType 로그아웃하는 클라이언트 종류
     */
    public void logOut(String refreshToken, ClientType clientType) {
        if (refreshToken == null) {
            return;
        }

        TokenInfo parsed;
        try {
            parsed = jwtProvider.parse(refreshToken);
        } catch (ResponseStatusException e) {
            return;
        }
        if (parsed.tokenType() != TokenType.REFRESH || parsed.clientType() != clientType) {
            return;
        }

        refreshTokenRepository
                .findTokenId(parsed.userId(), clientType)
                .filter(parsed.tokenId()::equals)
                .ifPresent(tokenId -> refreshTokenRepository.delete(parsed.userId(), clientType));
    }

    /**
     * 계정을 탈퇴 처리한다. 유저와 유저의 모든 데이터를 즉시 삭제하고, 남아있는 refresh·access 세션을 지운 뒤 아직 만료되지 않은 access 토큰도 막는다.
     * 이미 열려있는 GAME WebSocket·SSE 연결도 끊는다.
     *
     * <p>데이터 삭제는 {@link UserService#deleteAccount}의 트랜잭션으로 원자적으로 처리된다. 그 트랜잭션이 커밋된 뒤에 Redis 정리를
     * 하므로, 삭제가 실패하면 토큰은 그대로 유효하다.
     *
     * @param userId 탈퇴할 유저 ID
     * @throws org.springframework.web.server.ResponseStatusException 로그인하지 않았으면 UNAUTHORIZED
     */
    public void deleteUser(Long userId) {
        if (userId == null) {
            throw ErrorCode.UNAUTHORIZED.exception();
        }
        userService.deleteAccount(userId);
        refreshTokenRepository.delete(userId, ClientType.GAME);
        refreshTokenRepository.delete(userId, ClientType.WEB);
        accessTokenRepository.delete(userId, ClientType.GAME);
        accessTokenRepository.delete(userId, ClientType.WEB);
        tokenBlocklistRepository.block(userId);
        gameWebSocketSessionRegistry.disconnectExisting(userId);
        sseEmitterService.disconnectExistingGameSessions(userId);
    }
}
