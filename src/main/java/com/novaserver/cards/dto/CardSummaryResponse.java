package com.novaserver.cards.dto;

import com.novaserver.cards.entity.UpgradeCard;

/** 카드 목록 응답. */
public record CardSummaryResponse(
        Long id, String name, Long skillId, String skillName, boolean enabled, int maxPickCount) {

    public static CardSummaryResponse from(UpgradeCard card) {
        return new CardSummaryResponse(
                card.getId(),
                card.getName(),
                card.getSkill().getId(),
                card.getSkill().getName(),
                card.isEnabled(),
                card.getMaxPickCount());
    }
}
