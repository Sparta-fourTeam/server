package com.novaserver.quest.dto;

import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;
import java.time.Instant;
import java.util.List;

/** 현재 서버 기간에 속한 계정 진행·점수·상자 복원 응답. */
public record QuestBoardResponse(
        QuestPeriod period,
        String periodKey,
        Instant nextResetAt,
        String lastLoginDay,
        int loginCount,
        int spentEnergy,
        long points,
        List<Row> quests,
        List<Chest> chests) {
    /** 완료와 수령을 구분하는 퀘스트 행. */
    public record Row(
            String id,
            QuestKind kind,
            int count,
            int target,
            int points,
            boolean claimed,
            Instant claimedAt) {}

    /** 점수 조건과 상자 수령 상태. */
    public record Chest(String id, int points, boolean claimed, Instant claimedAt) {}
}
