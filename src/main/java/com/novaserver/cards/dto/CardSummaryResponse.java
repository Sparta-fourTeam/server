package com.novaserver.cards.dto;

import com.novaserver.cards.entity.UpgradeCard;

/** 카드 목록 응답. */
public record CardSummaryResponse(
        Long id, String name, Long skillId, String skillName, boolean enabled, int maxPickCount) {

    /** 카드 엔티티를 목록용 요약 응답으로 변환한다. */
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
