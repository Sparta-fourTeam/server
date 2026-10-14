package com.novaserver.cards.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.cards.entity.UpgradeCard;
import java.util.List;

/** 강화 카드 상세 응답. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardDetailResponse(
        Long id,
        NamedRef skill,
        String name,
        String desc,
        int maxPickCount,
        boolean enabled,
        String disabledReason,
        int minBattleLevel,
        int minPermanentLevel,
        List<RequiredCardCountResponse> requiredCardCounts,
        List<CardExclusionResponse> exclusions,
        List<RequiredSkillResponse> requiredSkills,
        String sharedId,
        List<CardEffectResponse> effects,
        List<CardVariantResponse> variants) {

    /** 연관 테이블 값. */
    public record NamedRef(Long id, String name) {}

    /** 카드 엔티티를 상세 응답으로 변환한다. */
    public static CardDetailResponse from(UpgradeCard card) {
        return new CardDetailResponse(
                card.getId(),
                new NamedRef(card.getSkill().getId(), card.getSkill().getName()),
                card.getName(),
                card.getDesc(),
                card.getMaxPickCount(),
                card.isEnabled(),
                card.getDisabledReason(),
                card.getMinBattleLevel(),
                card.getMinPermanentLevel(),
                card.getRequiredCardCounts().stream().map(RequiredCardCountResponse::from).toList(),
                card.getExclusions().stream().map(CardExclusionResponse::from).toList(),
                card.getRequiredSkills().stream().map(RequiredSkillResponse::from).toList(),
                card.getSharedId(),
                card.getEffects().stream().map(CardEffectResponse::from).toList(),
                card.getVariants().stream().map(CardVariantResponse::from).toList());
    }
}
