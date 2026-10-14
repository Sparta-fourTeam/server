package com.novaserver.cards.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.cards.entity.RequiredCardCount;

/** 유니티용 선행 카드 조건 데이터 응답. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RequiredCardCountDataResponse(Long cardId, int count) {

    /** 선행 카드 조건 엔티티를 유니티용 데이터로 변환한다. */
    public static RequiredCardCountDataResponse from(RequiredCardCount requiredCardCount) {
        return new RequiredCardCountDataResponse(
                requiredCardCount.getRequiredCard().getId(), requiredCardCount.getCount());
    }
}
