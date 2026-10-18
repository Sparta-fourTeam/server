package com.novaserver.playerlevel.dto;

import com.novaserver.playerlevel.entity.PlayerLevelConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** 플레이어 경험치 곡선 전체 교체 요청. 모든 항목을 명시해야 한다. */
public record PlayerLevelConfigRequest(
        @NotNull @Positive @Schema(description = "레벨 1에서 2에 필요한 경험치. 필수 양수") Integer baseExp,
        @NotNull @PositiveOrZero @Schema(description = "다음 레벨마다 증가하는 필요 경험치. 0도 명시해야 함")
                Integer stepExp,
        @NotNull
                @Min(2)
                @Max(PlayerLevelConfig.MAX_SUPPORTED_LEVEL)
                @Schema(description = "도달 가능한 최고 플레이어 레벨. 필수, 2~1000")
                Integer maxLevel) {}
