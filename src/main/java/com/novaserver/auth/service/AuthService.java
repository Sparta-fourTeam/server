package com.novaserver.auth.service;

import com.novaserver.auth.dto.GuestSignUpResponse;
import com.novaserver.auth.dto.TokenResponse;
import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.IssuedToken;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.jwt.TokenInfo;
import com.novaserver.auth.jwt.TokenType;
import com.novaserver.auth.repository.RefreshTokenRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.Provider;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게스트 가입·로그인, 토큰 발급·재발급을 담당한다. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 새 게스트 유저를 만들고 토큰을 발급한다. DB에는 guestId의 해시만 저장한다.
     *
     * @return 원본 guestId와 access·refresh 토큰
     */
    @Transactional
    public GuestSignUpResponse guestSignUp() {
        String guestId = UUID.randomUUID().toString();
        User user = userRepository.save(new User(Provider.GUEST, hash(guestId)));
        TokenResponse tokens = issueTokens(user.getId(), ClientType.GAME);
        return new GuestSignUpResponse(guestId, tokens.getAccessToken(), tokens.getRefreshToken());
    }

    /**
     * 게스트 ID로 게스트를 찾아 토큰을 발급한다. 없는 게스트면 새로 만들지 않는다.
     *
     * @param guestId 가입 때 받은 게스트 ID
     * @return access·refresh 토큰
     * @throws org.springframework.web.server.ResponseStatusException UUID 형식이 아니면 INVALID_REQUEST,
     *     일치하는 게스트가 없으면 INVALID_GUEST_ID
     */
    @Transactional(readOnly = true)
    public TokenResponse guestLogIn(String guestId) {
        String hashed = hash(parseUuid(guestId).toString());
        User user =
                userRepository
                        .findByProviderAndAccountId(Provider.GUEST, hashed)
                        .orElseThrow(ErrorCode.INVALID_GUEST_ID::exception);
        return issueTokens(user.getId(), ClientType.GAME);
    }

    /**
     * Refresh 토큰을 검증하고 새 access·refresh 토큰을 발급한다. 기존 refresh는 새 값으로 덮어써져 무효가 된다.
     *
     * <p>저장된 값과 다른 refresh가 오면 탈취로 보고 저장된 refresh를 삭제한다. 이후 해당 유저는 다시 로그인해야 한다.
     *
     * @param refreshToken 클라이언트가 보관하던 refresh 토큰
     * @return 새 access·refresh 토큰
     * @throws org.springframework.web.server.ResponseStatusException 만료되면 TOKEN_EXPIRED, refresh
     *     토큰이 아니거나 저장된 값이 없거나 일치하지 않으면 INVALID_TOKEN
     */
    public TokenResponse refresh(String refreshToken) {
        TokenInfo parsed = jwtProvider.parse(refreshToken);
        if (parsed.tokenType() != TokenType.REFRESH) {
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

        return issueTokens(parsed.userId(), parsed.clientType());
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

    private TokenResponse issueTokens(Long userId, ClientType clientType) {
        String access = jwtProvider.createAccessToken(userId, clientType);
        IssuedToken refresh = jwtProvider.createRefreshToken(userId, clientType);
        refreshTokenRepository.save(userId, clientType, refresh.tokenId());
        return new TokenResponse(access, refresh.token());
    }
}
