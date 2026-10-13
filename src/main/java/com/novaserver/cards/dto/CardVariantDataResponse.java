package com.novaserver.cards.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.cards.entity.CardVariant;
import java.util.List;

/** 유니티용 카드 변형 데이터 응답. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardVariantDataResponse(
        int minPermanentLevel, String name, String desc, List<CardEffectDataResponse> effects) {

    public static CardVariantDataResponse from(CardVariant variant) {
        return new CardVariantDataResponse(
                variant.getMinPermanentLevel(),
                variant.getName(),
                variant.getDesc(),
                variant.getEffects().stream().map(CardEffectDataResponse::from).toList());
    }
}
