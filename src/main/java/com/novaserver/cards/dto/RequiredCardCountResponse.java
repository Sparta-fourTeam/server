package com.novaserver.cards.dto;

import com.novaserver.cards.entity.RequiredCardCount;

/** 선행 카드 조건 응답. */
public record RequiredCardCountResponse(Long id, NamedRef requiredCard, NamedRef skill, int count) {

    /** 연관 테이블 값. */
    public record NamedRef(Long id, String name) {}

    public static RequiredCardCountResponse from(RequiredCardCount requiredCardCount) {
        return new RequiredCardCountResponse(
                requiredCardCount.getId(),
                new NamedRef(
                        requiredCardCount.getRequiredCard().getId(),
                        requiredCardCount.getRequiredCard().getName()),
                requiredCardCount.getSkill() != null
                        ? new NamedRef(
                                requiredCardCount.getSkill().getId(),
                                requiredCardCount.getSkill().getName())
                        : null,
                requiredCardCount.getCount());
    }
}
