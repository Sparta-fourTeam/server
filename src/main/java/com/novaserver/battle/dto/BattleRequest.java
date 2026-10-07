package com.novaserver.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 배틀 종료 요청. */
@Getter
@Schema(description = "배틀 종료 요청")
public class BattleRequest {
    @Schema(
            description = "유저 ID (JWT 적용 전 임시)",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "유저 ID는 필수입니다")
    private Long userId;

    @Schema(
            description = "발급받은 배틀 ID",
            example = "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "배틀 ID는 필수입니다")
    private String battleId;
}
