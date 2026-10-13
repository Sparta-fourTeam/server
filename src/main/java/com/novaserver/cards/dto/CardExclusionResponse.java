package com.novaserver.cards.dto;

import com.novaserver.cards.entity.CardExclusion;

/** 카드 배제 조건 응답. */
public record CardExclusionResponse(
        Long id, NamedRef excludingCard, NamedRef skill, int belowPermanentLevel) {

    /** 연관 테이블 값. */
    public record NamedRef(Long id, String name) {}

    public static CardExclusionResponse from(CardExclusion exclusion) {
        return new CardExclusionResponse(
                exclusion.getId(),
                new NamedRef(
                        exclusion.getExcludingCard().getId(),
                        exclusion.getExcludingCard().getName()),
                exclusion.getSkill() != null
                        ? new NamedRef(exclusion.getSkill().getId(), exclusion.getSkill().getName())
                        : null,
                exclusion.getBelowPermanentLevel());
    }
}
