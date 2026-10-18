package com.novaserver.stage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;

/** 스테이지 보상 아이템 등록·수정 요청. */
@Getter
public class StageRewardItemRequest {
    @NotBlank(message = "itemId는 필수값입니다.")
    @Schema(description = "RewardRules의 실제 아이템 ID 또는 별도 랜덤 추첨 ID. 추첨 ID는 종료 시 실제 재료로 확정")
    private String itemId;

    @NotNull(message = "finalAmount는 필수값입니다.")
    @PositiveOrZero(message = "finalAmount는 0 이상입니다.")
    private Integer finalAmount;
}
