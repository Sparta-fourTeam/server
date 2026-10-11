package com.novaserver.auth.controller;

import com.novaserver.auth.dto.GuestLogInRequest;
import com.novaserver.auth.dto.GuestSignUpResponse;
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

/** 게스트 가입·로그인 API. */
@Tag(name = "인증", description = "게스트 가입 및 로그인 API")
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
}
