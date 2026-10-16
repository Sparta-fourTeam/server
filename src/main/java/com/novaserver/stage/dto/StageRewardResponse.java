package com.novaserver.stage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.stage.entity.StageReward;
import java.util.List;

/** 웹용 스테이지 보상 응답. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StageRewardResponse(
        Integer stageId,
        int baseExp,
        int threeStarBonusExp,
        int finalCoin,
        List<StageRewardItemResponse> items) {

    /** 스테이지 보상 엔티티로부터 응답을 만든다. */
    public static StageRewardResponse from(StageReward reward) {
        return new StageRewardResponse(
                reward.getStageId(),
                reward.getBaseExp(),
                reward.getThreeStarBonusExp(),
                reward.getFinalCoin(),
                reward.getItems().stream().map(StageRewardItemResponse::from).toList());
    }
}
