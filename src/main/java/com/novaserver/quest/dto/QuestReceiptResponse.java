package com.novaserver.quest.dto;

import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.entity.UserQuestClaim;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** 재전송에 반환하는 최초 점수·실제 보상·UTC 수령 시각. 현재 잔액과 구별한다. */
public record QuestReceiptResponse(
        String id,
        QuestPeriod period,
        String periodKey,
        boolean chest,
        int points,
        int gold,
        int energy,
        int gems,
        Map<String, Integer> items,
        Instant claimedAt) {
    /** 저장된 최초 결과를 복원한다. */
    public static QuestReceiptResponse from(UserQuestClaim c) {
        return new QuestReceiptResponse(
                c.getDefinitionId(),
                c.getPeriod(),
                c.getPeriodDay().format(DateTimeFormatter.BASIC_ISO_DATE),
                c.isChest(),
                c.getPoints(),
                c.getGold(),
                c.getEnergy(),
                c.getGems(),
                Map.copyOf(c.getItems()),
                c.getClaimedAt());
    }
}
