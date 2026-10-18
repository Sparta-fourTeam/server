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
    @Schema(description = "클라와 공통 지급 규칙이 있는 고정 스킬/장비 재료 또는 chest.gem. 랜덤 재료 확정 지급은 별도 준비 중")
    private String itemId;

    @NotNull(message = "finalAmount는 필수값입니다.")
    @PositiveOrZero(message = "finalAmount는 0 이상입니다.")
    private Integer finalAmount;
}
