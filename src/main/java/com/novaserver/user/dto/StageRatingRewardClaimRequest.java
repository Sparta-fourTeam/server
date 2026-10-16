package com.novaserver.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 별등급 보상 수령 요청. */
@Getter
public class StageRatingRewardClaimRequest {
    @NotNull(message = "stageId는 필수값입니다.")
    private Integer stageId;
}
