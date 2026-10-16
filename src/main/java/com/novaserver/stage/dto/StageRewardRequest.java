package com.novaserver.stage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.Getter;

/** 스테이지 보상 등록·수정 요청. */
@Getter
public class StageRewardRequest {
    @NotNull(message = "baseExp는 필수값입니다.")
    @PositiveOrZero(message = "baseExp는 0 이상입니다.")
    private Integer baseExp;

    @NotNull(message = "threeStarBonusExp는 필수값입니다.")
    @PositiveOrZero(message = "threeStarBonusExp는 0 이상입니다.")
    private Integer threeStarBonusExp;

    @NotNull(message = "finalCoin은 필수값입니다.")
    @Min(value = 125, message = "finalCoin은 125 이상입니다.")
    private Integer finalCoin;

    @Valid private List<StageRewardItemRequest> items;
}
