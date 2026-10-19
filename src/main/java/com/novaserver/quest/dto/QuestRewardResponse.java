package com.novaserver.quest.dto;

import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.entity.QuestReward;

/** 퀘스트 보상 응답. */
public record QuestRewardResponse(
        String rewardId, QuestPeriod period, int points, int gold, int energy) {

    /** 보상 엔티티로부터 응답을 만든다. */
    public static QuestRewardResponse from(QuestReward reward) {
        return new QuestRewardResponse(
                reward.getRewardId(),
                reward.getPeriod(),
                reward.getPoints(),
                reward.getGold(),
                reward.getEnergy());
    }
}
