package com.novaserver.cards.dto;

import com.novaserver.cards.entity.CardVariant;
import java.util.List;

/** 카드 변형 응답. */
public record CardVariantResponse(
        Long id,
        int minPermanentLevel,
        String name,
        String desc,
        List<CardEffectResponse> effects) {

    /** 변형 엔티티를 응답으로 변환한다. */
    public static CardVariantResponse from(CardVariant variant) {
        return new CardVariantResponse(
                variant.getId(),
                variant.getMinPermanentLevel(),
                variant.getName(),
                variant.getDesc(),
                variant.getEffects().stream().map(CardEffectResponse::from).toList());
    }
}
