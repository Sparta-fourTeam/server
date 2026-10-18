package com.novaserver.quest.dto;

import com.novaserver.quest.entity.QuestChest;
import com.novaserver.quest.entity.QuestPeriod;

/** 관리자 상자 조건과 보상 연결 응답. */
public record QuestChestResponse(
        String id, QuestPeriod period, int points, Long rewardDefinitionId, int energy, int gems) {
    /** 서버 상자 정의를 DTO로 변환한다. */
    public static QuestChestResponse from(QuestChest c) {
        return new QuestChestResponse(
                c.getId(),
                c.getPeriod(),
                c.getPoints(),
                c.getRewardDefinition().getId(),
                c.getEnergy(),
                c.getGems());
    }
}
