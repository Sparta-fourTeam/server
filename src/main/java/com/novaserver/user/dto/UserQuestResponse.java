package com.novaserver.user.dto;

import com.novaserver.quest.entity.Quest;
import com.novaserver.quest.entity.QuestKind;

/** 유저 퀘스트 진행도 응답. */
public record UserQuestResponse(
        String questId,
        QuestKind kind,
        int target,
        int points,
        int count,
        boolean completed,
        boolean claimed) {

    /** 퀘스트 정의와 유저의 진행도를 합쳐 응답을 만든다. */
    public static UserQuestResponse from(
            Quest quest, int count, boolean completed, boolean claimed) {
        return new UserQuestResponse(
                quest.getQuestId(),
                quest.getKind(),
                quest.getTarget(),
                quest.getPoints(),
                count,
                completed,
                claimed);
    }
}
