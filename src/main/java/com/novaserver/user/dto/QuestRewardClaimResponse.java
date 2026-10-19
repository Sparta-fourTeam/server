package com.novaserver.user.dto;

import com.novaserver.quest.entity.QuestPeriod;

/** 퀘스트 보상 수령 응답. */
public record QuestRewardClaimResponse(
        QuestPeriod period, int points, int claimedPoints, int grantedGold, int grantedEnergy) {}
