package com.novaserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** 토큰 재발급 요청. */
@Schema(description = "토큰 재발급 요청")
@Getter
public class RefreshRequest {
    @Schema(description = "로그인·가입·재발급 때 받은 refresh 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "refresh 토큰은 필수입니다")
    private String refreshToken;
}
