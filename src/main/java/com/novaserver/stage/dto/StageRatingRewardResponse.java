package com.novaserver.stage.dto;

import com.novaserver.stage.entity.StageRatingReward;
import java.util.Map;

/** 별등급 보상 응답. */
public record StageRatingRewardResponse(
        Integer stageId,
        int rating,
        int gold,
        Map<String, Integer> items,
        Long rewardDefinitionId,
        Integer rewardDefinitionVersion) {

    /** 별등급 보상 엔티티로부터 응답을 만든다. */
    public static StageRatingRewardResponse from(StageRatingReward reward) {
        return new StageRatingRewardResponse(
                reward.getStage().getStageId(),
                reward.getRating(),
                reward.getGold(),
                reward.getItems(),
                reward.getRewardDefinition().getId(),
                reward.getRewardDefinition().getVersion());
    }
}
