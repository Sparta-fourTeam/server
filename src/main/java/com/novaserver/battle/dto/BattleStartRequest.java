package com.novaserver.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 배틀 시작 요청. */
// TODO: Jwt 반영 후 삭제 예정
@Schema(description = "배틀 시작 요청")
@Getter
public class BattleStartRequest {
    @Schema(
            description = "유저 ID (JWT 적용 전 임시)",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "유저 ID는 필수입니다")
    private Long userId;
}
