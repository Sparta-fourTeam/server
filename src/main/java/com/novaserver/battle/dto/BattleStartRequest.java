package com.novaserver.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 배틀 시작 요청. */
@Schema(description = "배틀 시작 요청")
@Getter
public class BattleStartRequest {
    // TODO: JWT 적용 후 이 클래스 삭제
    @Schema(
            description = "유저 ID (JWT 적용 전 임시)",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "유저 ID는 필수입니다")
    private Long userId;
}
