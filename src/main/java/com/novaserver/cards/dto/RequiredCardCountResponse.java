package com.novaserver.cards.dto;

import com.novaserver.cards.entity.RequiredCardCount;

/** 선행 카드 조건 응답. */
public record RequiredCardCountResponse(Long id, NamedRef requiredCard, int count) {

    /** 연관 테이블 값. */
    public record NamedRef(Long id, String name) {}

    /** 선행 카드 조건 엔티티를 응답으로 변환한다. */
    public static RequiredCardCountResponse from(RequiredCardCount requiredCardCount) {
        return new RequiredCardCountResponse(
                requiredCardCount.getId(),
                new NamedRef(
                        requiredCardCount.getRequiredCard().getId(),
                        requiredCardCount.getRequiredCard().getName()),
                requiredCardCount.getCount());
    }
}
