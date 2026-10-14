package com.novaserver.cards.dto;

import com.novaserver.cards.entity.EffectTrigger;
import java.time.LocalDateTime;
import lombok.Getter;

/** 효과 발동 시점 응답. */
@Getter
public class EffectTriggerResponse {
    private final Long id;
    private final String name;
    private final String label;
    private final LocalDateTime updatedAt;

    /** 효과 발동 시점 엔티티로 응답을 만든다. */
    public EffectTriggerResponse(EffectTrigger trigger) {
        this.id = trigger.getId();
        this.name = trigger.getName();
        this.label = trigger.getLabel();
        this.updatedAt = trigger.getUpdatedAt();
    }
}
