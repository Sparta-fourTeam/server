package com.novaserver.quest.dto;

import com.novaserver.quest.entity.QuestPeriod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 관리자 전용 상자 조건과 공통 보상 참조 및 추가 재화. */
public record QuestChestRequest(
        @NotNull QuestPeriod period,
        @Min(1) int points,
        @NotNull @Min(1) Long rewardDefinitionId,
        @NotNull @Min(0) Integer energy,
        @NotNull @Min(0) Integer gems) {}
