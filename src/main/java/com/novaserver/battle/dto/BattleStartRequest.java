package com.novaserver.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** 배틀 시작 요청. */
@Schema(description = "배틀 시작 요청")
@Getter
public class BattleStartRequest {
    @Schema(
            description = "클라이언트가 생성한 멱등성 키. 같은 키로 재요청하면 기존 배틀 ID를 그대로 돌려받는다.",
            example = "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "멱등성 키는 필수입니다")
    private String createId;
}
