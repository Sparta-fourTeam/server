package com.novaserver.auth.dto;

import com.novaserver.user.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 소셜 로그인 요청. */
@Schema(description = "소셜 로그인 요청")
@Getter
public class SocialLogInRequest {
    @Schema(
            description = "ID 토큰을 발급한 소셜 provider (GOOGLE, APPLE)",
            example = "GOOGLE",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "provider는 필수입니다")
    private Provider provider;

    @Schema(
            description = "클라이언트가 Google·Apple 로그인으로 받은 ID 토큰 원문",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "idToken은 필수입니다")
    private String idToken;
}
