package com.novaserver.cards.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.cards.entity.UpgradeCard;
import java.util.List;

/** 유니티용 강화 카드 데이터 응답. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardDataResponse(
        Long id,
        String name,
        String desc,
        int maxPickCount,
        boolean enabled,
        String disabledReason,
        int minBattleLevel,
        int minPermanentLevel,
        List<RequiredCardCountDataResponse> requiredCardCounts,
        List<CardExclusionDataResponse> exclusions,
        String sharedId,
        List<CardEffectDataResponse> effects,
        List<CardVariantDataResponse> variants) {

    /** 카드 엔티티를 유니티용 카드 데이터로 변환한다. */
    public static CardDataResponse from(UpgradeCard card) {
        return new CardDataResponse(
                card.getId(),
                card.getName(),
                card.getDesc(),
                card.getMaxPickCount(),
                card.isEnabled(),
                card.getDisabledReason(),
                card.getMinBattleLevel(),
                card.getMinPermanentLevel(),
                card.getRequiredCardCounts().stream()
                        .map(RequiredCardCountDataResponse::from)
                        .toList(),
                card.getExclusions().stream().map(CardExclusionDataResponse::from).toList(),
                card.getSharedId(),
                card.getEffects().stream().map(CardEffectDataResponse::from).toList(),
                card.getVariants().stream().map(CardVariantDataResponse::from).toList());
    }
}
