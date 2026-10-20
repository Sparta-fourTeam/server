package com.novaserver.auth.controller;

import com.novaserver.auth.dto.AccessTokenResponse;
import com.novaserver.auth.dto.GuestLogInRequest;
import com.novaserver.auth.dto.GuestSignUpResponse;
import com.novaserver.auth.dto.RefreshRequest;
import com.novaserver.auth.dto.SocialLogInRequest;
import com.novaserver.auth.dto.TokenResponse;
import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.RefreshCookieFactory;
import com.novaserver.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게스트·소셜 로그인, 토큰 재발급, Web 로그인·로그아웃 API. */
@Tag(name = "인증", description = "게스트·소셜 로그인, 토큰 재발급, Web 로그인·로그아웃 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

    @Operation(
            summary = "게스트 가입",
            description = "새 게스트 계정을 만들고 guestId와 토큰을 발급합니다. guestId는 이 응답에서만 내려갑니다.")
    @PostMapping("/guest/signup")
    public GuestSignUpResponse guestSignUp() {
        return authService.guestSignUp();
    }

    @Operation(
            summary = "게스트 로그인",
            description = "guestId로 토큰을 발급합니다. UUID 형식이 아니면 400, 일치하는 게스트가 없으면 401을 반환합니다.")
    @PostMapping("/guest/login")
    public TokenResponse guestLogIn(@Valid @RequestBody GuestLogInRequest request) {
        return authService.guestLogIn(request.getGuestId());
    }

    @Operation(
            summary = "소셜 로그인",
            description =
                    "Google·Apple ID 토큰을 검증하고 토큰을 발급합니다. 처음 로그인한 계정이면 자동으로 가입합니다. "
                            + "forceLogin이 false이고 이미 다른 기기에서 로그인 중이면 409를 반환합니다. "
                            + "forceLogin이 true면 기존 세션을 끊고 로그인합니다. "
                            + "지원하지 않는 provider면 400, ID 토큰 검증에 실패하면 401, "
                            + "Google·Apple 공개키 서버에 연결할 수 없으면 503을 반환합니다.")
    @PostMapping("/social/login")
    public TokenResponse socialLogIn(@Valid @RequestBody SocialLogInRequest request) {
        return authService.socialLogIn(
                request.getIdToken(),
                request.getProvider(),
                ClientType.GAME,
                request.isForceLogin());
    }

    @Operation(
            summary = "소셜 계정 연동",
            description =
                    "로그인한 게스트 계정에 Google·Apple 계정을 연동합니다. "
                            + "로그인하지 않았으면 401, 이미 소셜 계정이 연동된 유저면 409, "
                            + "연동하려는 소셜 계정이 이미 다른 유저에 연동되어 있으면 409를 반환합니다.")
    @PostMapping("/social/link")
    public ResponseEntity<Void> linkSocialAccount(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody SocialLogInRequest request) {
        authService.linkSocialAccount(userId, request.getIdToken(), request.getProvider());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "토큰 재발급",
            description =
                    "refresh 토큰으로 새 access·refresh 토큰을 발급하고 기존 refresh는 무효로 합니다. "
                            + "만료됐거나, refresh 토큰이 아니거나, 이미 사용한 토큰이면 401을 반환합니다. "
                            + "이미 사용한 토큰이 오면 저장된 refresh도 삭제되어 다시 로그인해야 합니다.")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.getRefreshToken(), ClientType.GAME);
    }

    @Operation(
            summary = "게임쪽 로그아웃",
            description = "저장된 refresh 토큰을 무효로 합니다. 토큰이 없거나 만료됐거나 이미 사용한 토큰이어도 항상 204를 반환합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logOut(@Valid @RequestBody RefreshRequest request) {
        authService.logOut(request.getRefreshToken(), ClientType.GAME);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Web 로그인",
            description =
                    "Google·Apple ID 토큰으로 Web 토큰을 발급합니다. access 토큰은 body로, "
                            + "refresh 토큰은 HttpOnly 쿠키(refresh_token)로 내려갑니다. "
                            + "실패 조건은 소셜 로그인과 같습니다.")
    @PostMapping("/web/login")
    public ResponseEntity<AccessTokenResponse> webLogIn(
            @Valid @RequestBody SocialLogInRequest request) {
        TokenResponse tokens =
                authService.socialLogIn(
                        request.getIdToken(), request.getProvider(), ClientType.WEB);
        return withRefreshCookie(tokens);
    }

    @Operation(
            summary = "Web 토큰 재발급",
            description =
                    "refresh_token 쿠키로 새 토큰을 발급합니다. access 토큰은 body로, 새 refresh 토큰은 쿠키로 "
                            + "내려갑니다. 쿠키가 없거나, 만료됐거나, Web 토큰이 아니거나, 이미 사용한 토큰이면 "
                            + "401을 반환합니다.")
    @PostMapping("/web/refresh")
    public ResponseEntity<AccessTokenResponse> webRefresh(
            @CookieValue(name = RefreshCookieFactory.NAME, required = false) String refreshToken) {
        return withRefreshCookie(authService.refresh(refreshToken, ClientType.WEB));
    }

    @Operation(
            summary = "Web 로그아웃",
            description =
                    "저장된 refresh 토큰을 무효로 하고 refresh_token 쿠키를 지웁니다. "
                            + "쿠키가 없거나 이미 만료된 경우에도 항상 204를 반환합니다.")
    @PostMapping("/web/logout")
    public ResponseEntity<Void> webLogOut(
            @CookieValue(name = RefreshCookieFactory.NAME, required = false) String refreshToken) {
        authService.webLogOut(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.expire().toString())
                .build();
    }

    @Operation(
            summary = "회원 탈퇴",
            description =
                    "계정과 계정에 속한 모든 데이터를 즉시 삭제합니다. 발급된 모든 refresh 토큰을 무효로 하고, "
                            + "남은 access 토큰도 만료 전까지 차단합니다. 로그인하지 않았으면 401을 반환합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal Long userId) {
        authService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AccessTokenResponse> withRefreshCookie(TokenResponse tokens) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookieFactory.create(tokens.getRefreshToken()).toString())
                .body(new AccessTokenResponse(tokens.getAccessToken()));
    }
}
