package com.novaserver.user.dto;

import com.novaserver.quest.entity.QuestReward;

/** 유저 기준 퀘스트 보상 응답. */
public record UserQuestRewardResponse(
        String rewardId, int points, int gold, int energy, boolean unlocked, boolean claimed) {

    /** 보상 정의와 유저의 수령 현황을 합쳐 응답을 만든다. */
    public static UserQuestRewardResponse from(
            QuestReward reward, boolean unlocked, boolean claimed) {
        return new UserQuestRewardResponse(
                reward.getRewardId(),
                reward.getPoints(),
                reward.getGold(),
                reward.getEnergy(),
                unlocked,
                claimed);
    }
}
