package com.novaserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 토큰 발급 응답. */
@Getter
@RequiredArgsConstructor
@Schema(description = "토큰 발급 응답")
public class TokenResponse {
    @Schema(description = "access 토큰")
    private final String accessToken;

    @Schema(description = "refresh 토큰")
    private final String refreshToken;
}
