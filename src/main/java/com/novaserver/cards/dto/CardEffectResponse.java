package com.novaserver.cards.dto;

import com.novaserver.cards.entity.CardEffect;
import java.util.Map;

/** 카드 효과 응답. */
public record CardEffectResponse(
        Long id,
        int sortOrder,
        String kind,
        double value,
        String form,
        String trigger,
        Long skillId,
        String skillName,
        double chance,
        double damageScale,
        Long targetSkillId,
        String targetSkillName,
        int count,
        String onlyForm,
        boolean excludeHit,
        Double interval,
        Map<String, Double> inherit) {

    /** 효과 엔티티를 응답으로 변환한다. */
    public static CardEffectResponse from(CardEffect effect) {
        return new CardEffectResponse(
                effect.getId(),
                effect.getSortOrder(),
                effect.getKind().getName(),
                effect.getValue(),
                effect.getForm() != null ? effect.getForm().getName() : null,
                effect.getTrigger() != null ? effect.getTrigger().getName() : null,
                effect.getSkill() != null ? effect.getSkill().getId() : null,
                effect.getSkill() != null ? effect.getSkill().getName() : null,
                effect.getChance(),
                effect.getDamageScale(),
                effect.getTarget() != null ? effect.getTarget().getId() : null,
                effect.getTarget() != null ? effect.getTarget().getName() : null,
                effect.getCount(),
                effect.getOnlyForm() != null ? effect.getOnlyForm().getName() : null,
                effect.isExcludeHit(),
                effect.getInterval(),
                effect.getInherit());
    }
}
