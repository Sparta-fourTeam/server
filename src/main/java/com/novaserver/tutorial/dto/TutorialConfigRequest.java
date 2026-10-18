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
        @Min(0) int upgradeLevel,
        @Min(0) int playerLevel,
        @Schema(description = "0은 경험치 유지. 양수는 최초 완료 시 해당 레벨 시작 누적 경험치로 맞춤") @Min(0) int setLevel,
        @NotNull @Positive Long rewardDefinitionId) {}
