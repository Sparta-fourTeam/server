package com.novaserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** 게스트 로그인 요청. */
@Schema(description = "게스트 로그인 요청")
@Getter
public class GuestLogInRequest {
    @Schema(
            description = "가입 때 받은 게스트 ID",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "게스트 ID는 필수입니다")
    private String guestId;
}
