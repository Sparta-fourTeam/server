package com.novaserver.cards.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.cards.entity.CardEffect;

/** 유니티용 카드 효과 데이터 응답. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardEffectDataResponse(
        String kind,
        double value,
        String form,
        String trigger,
        Long skillId,
        double chance,
        double damageScale,
        Long target,
        int count,
        String onlyForm,
        boolean excludeHit,
        Double interval) {

    public static CardEffectDataResponse from(CardEffect effect) {
        return new CardEffectDataResponse(
                effect.getKind().getName(),
                effect.getValue(),
                effect.getForm() != null ? effect.getForm().getName() : null,
                effect.getTrigger() != null ? effect.getTrigger().getName() : null,
                effect.getSkill() != null ? effect.getSkill().getId() : null,
                effect.getChance(),
                effect.getDamageScale(),
                effect.getTarget() != null ? effect.getTarget().getId() : null,
                effect.getCount(),
                effect.getOnlyForm() != null ? effect.getOnlyForm().getName() : null,
                effect.isExcludeHit(),
                effect.getInterval());
    }
}
