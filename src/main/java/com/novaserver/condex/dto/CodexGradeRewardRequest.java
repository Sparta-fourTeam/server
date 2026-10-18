package com.novaserver.condex.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 등급에 연결할 기존 공통 보상 ID와 현재 등급 설정 버전. */
public record CodexGradeRewardRequest(
        @NotNull @Positive Long rewardDefinitionId, Integer expectedVersion) {}
