package com.novaserver.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Map;

/** 고정 보상 묶음의 전체 교체 입력. 공유 편집은 현재 버전을 요구한다. */
public record RewardDefinitionRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @PositiveOrZero Integer gold,
        Map<@NotBlank String, @NotNull @Positive Integer> items,
        @Schema(description = "PUT 필수: 조회한 현재 버전. POST에서는 사용하지 않음") Integer expectedVersion) {}
