package com.novaserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 게스트 가입 응답. guestId는 이 응답에서만 원본으로 내려간다. */
@Getter
@RequiredArgsConstructor
@Schema(description = "게스트 가입 응답")
public class GuestSignUpResponse {
    @Schema(
            description = "게스트 ID. 클라이언트가 저장해 두고 다음 로그인에 사용한다",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private final String guestId;

    @Schema(description = "access 토큰")
    private final String accessToken;

    @Schema(description = "refresh 토큰")
    private final String refreshToken;
}
