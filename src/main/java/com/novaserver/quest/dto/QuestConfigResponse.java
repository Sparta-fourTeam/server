package com.novaserver.quest.dto;

import com.novaserver.quest.entity.QuestDefinition;
import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;

/** 관리자 퀘스트 정의 응답. */
public record QuestConfigResponse(
        String id, QuestPeriod period, QuestKind kind, int target, int points) {
    /** 서버 정의를 DTO로 변환한다. */
    public static QuestConfigResponse from(QuestDefinition q) {
        return new QuestConfigResponse(
                q.getId(), q.getPeriod(), q.getKind(), q.getTarget(), q.getPoints());
    }
}
