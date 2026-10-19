package com.novaserver.quest.dto;

import com.novaserver.quest.entity.Quest;
import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;

/** 퀘스트 응답. */
public record QuestResponse(
        String questId, QuestPeriod period, QuestKind kind, int target, int points) {

    /** 퀘스트 엔티티로부터 응답을 만든다. */
    public static QuestResponse from(Quest quest) {
        return new QuestResponse(
                quest.getQuestId(),
                quest.getPeriod(),
                quest.getKind(),
                quest.getTarget(),
                quest.getPoints());
    }
}
