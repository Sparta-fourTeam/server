package com.novaserver.cards.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.cards.entity.UpgradeCard;
import java.util.List;

/** 유니티용 강화 카드 데이터 응답. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardDataResponse(
        Long id,
        int maxPickCount,
        boolean enabled,
        int minBattleLevel,
        int minPermanentLevel,
        List<RequiredCardCountDataResponse> requiredCardCounts,
        List<CardExclusionDataResponse> exclusions,
        List<Long> requiredSkillIds,
        String sharedId,
        List<CardEffectDataResponse> effects,
        List<CardVariantDataResponse> variants) {

    /** 엔티티로부터 응답을 만든다. */
    public static CardDataResponse from(UpgradeCard card) {
        return new CardDataResponse(
                card.getId(),
                card.getMaxPickCount(),
                card.isEnabled(),
                card.getMinBattleLevel(),
                card.getMinPermanentLevel(),
                card.getRequiredCardCounts().stream()
                        .map(RequiredCardCountDataResponse::from)
                        .toList(),
                card.getExclusions().stream().map(CardExclusionDataResponse::from).toList(),
                card.getRequiredSkills().stream()
                        .map(requiredSkill -> requiredSkill.getSkill().getId())
                        .toList(),
                card.getSharedId(),
                card.getEffects().stream().map(CardEffectDataResponse::from).toList(),
                card.getVariants().stream().map(CardVariantDataResponse::from).toList());
    }
}
