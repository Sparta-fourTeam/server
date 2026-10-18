package com.novaserver.stage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.Getter;

/** 스테이지 보상 등록·수정 요청. */
@Getter
public class StageRewardRequest {
    @NotNull(message = "baseExp는 필수값입니다.")
    @PositiveOrZero(message = "baseExp는 0 이상입니다.")
    @Schema(description = "완료 웨이브가 1 이상이면 한 번 지급하는 경험치")
    private Integer baseExp;

    @NotNull(message = "threeStarBonusExp는 필수값입니다.")
    @PositiveOrZero(message = "threeStarBonusExp는 0 이상입니다.")
    @Schema(description = "20웨이브 3별 완료 때 추가 지급하는 경험치")
    private Integer threeStarBonusExp;

    @NotNull(message = "finalCoin은 필수값입니다.")
    @PositiveOrZero(message = "finalCoin은 0 이상입니다.")
    private Integer finalCoin;

    @Valid private List<StageRewardItemRequest> items;
}
