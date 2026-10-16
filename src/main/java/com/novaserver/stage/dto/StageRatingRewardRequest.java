package com.novaserver.stage.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;

/** 별등급 보상 등록·수정 요청. */
@Getter
public class StageRatingRewardRequest {
    @NotNull(message = "gold는 필수값입니다.")
    @PositiveOrZero(message = "gold는 0 이상입니다.")
    private Integer gold;
}
