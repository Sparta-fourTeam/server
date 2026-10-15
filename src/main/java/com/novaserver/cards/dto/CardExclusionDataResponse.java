package com.novaserver.cards.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.cards.entity.CardExclusion;

/** 유니티용 카드 배제 조건 데이터 응답. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardExclusionDataResponse(Long cardId, int belowPermanentLevel) {

    /** 배제 조건 엔티티를 유니티용 데이터로 변환한다. */
    public static CardExclusionDataResponse from(CardExclusion exclusion) {
        return new CardExclusionDataResponse(
                exclusion.getExcludingCard().getId(), exclusion.getBelowPermanentLevel());
    }
}
