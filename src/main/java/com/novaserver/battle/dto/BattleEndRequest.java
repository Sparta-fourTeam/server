package com.novaserver.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 배틀 종료 요청. */
@Getter
@Schema(description = "배틀 종료 요청")
public class BattleEndRequest {
    @Schema(
            description = "발급받은 배틀 ID",
            example = "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "배틀 ID는 필수입니다")
    private String battleId;

    @Schema(description = "스테이지 id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "스테이지 id는 필수입니다")
    private Integer stageId;

    @Schema(description = "이번 배틀에서 클리어한 웨이브(0~20)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "클리어한 웨이브는 필수입니다")
    @Min(value = 0, message = "클리어한 웨이브는 0 이상이어야 합니다")
    @Max(value = 20, message = "클리어한 웨이브는 20 이하여야 합니다")
    private Integer clearedWave;

    @Schema(description = "이번 배틀의 클리어 등급(0~3)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "클리어 등급은 필수입니다")
    @Min(value = 0, message = "클리어 등급은 0 이상이어야 합니다")
    @Max(value = 3, message = "클리어 등급은 3 이하여야 합니다")
    private Integer rating;
}
