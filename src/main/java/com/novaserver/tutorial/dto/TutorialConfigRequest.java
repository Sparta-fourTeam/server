package com.novaserver.tutorial.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 관리자 전용 튜토리얼 검증 조건과 공통 고정 보상 연결 요청. */
public record TutorialConfigRequest(
        String prerequisiteId,
        @Min(1) int minLevel,
        @Positive Integer startStageId,
        @Positive Integer clearStageId,
        String upgradeId,
        @Schema(defaultValue = "0") @Min(0) Integer upgradeLevel,
        @Schema(defaultValue = "0") @Min(0) Integer playerLevel,
        @Schema(description = "0은 경험치 유지. 양수는 최초 완료 시 해당 레벨 시작 누적 경험치로 맞춤", defaultValue = "0")
                @Min(0)
                Integer setLevel,
        @NotNull @Positive Long rewardDefinitionId) {
    /** 사용하지 않는 선택 숫자 조건은 생략하거나 null이면 0으로 처리한다. */
    public TutorialConfigRequest {
        upgradeLevel = upgradeLevel == null ? 0 : upgradeLevel;
        playerLevel = playerLevel == null ? 0 : playerLevel;
        setLevel = setLevel == null ? 0 : setLevel;
    }
}
