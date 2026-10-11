package com.novaserver.auth.controller;

import com.novaserver.auth.dto.GuestLogInRequest;
import com.novaserver.auth.dto.GuestSignUpResponse;
import com.novaserver.auth.dto.RefreshRequest;
import com.novaserver.auth.dto.TokenResponse;
import com.novaserver.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게스트 가입·로그인과 토큰 재발급 API. */
@Tag(name = "인증", description = "게스트 가입·로그인 및 토큰 재발급 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

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
            summary = "토큰 재발급",
            description =
                    "refresh 토큰으로 새 access·refresh 토큰을 발급하고 기존 refresh는 무효로 합니다. "
                            + "만료됐거나, refresh 토큰이 아니거나, 이미 사용한 토큰이면 401을 반환합니다. "
                            + "이미 사용한 토큰이 오면 저장된 refresh도 삭제되어 다시 로그인해야 합니다.")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.getRefreshToken());
    }
}
