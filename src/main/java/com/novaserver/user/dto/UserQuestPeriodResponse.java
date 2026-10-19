package com.novaserver.user.dto;

import com.novaserver.quest.entity.QuestPeriod;
import java.util.List;

/** 주기별 퀘스트 진행도·보상 현황 응답. */
public record UserQuestPeriodResponse(
        QuestPeriod period,
        int points,
        int claimedPoints,
        List<UserQuestResponse> quests,
        List<UserQuestRewardResponse> rewards) {}
