package com.novaserver.cards.dto;

import com.novaserver.cards.entity.EffectKind;
import java.time.Instant;
import lombok.Getter;

/** 효과 종류 응답. */
@Getter
public class EffectKindResponse {
    private final Long id;
    private final String name;
    private final String label;
    private final Instant updatedAt;

    /** 효과 종류 엔티티로 응답을 만든다. */
    public EffectKindResponse(EffectKind kind) {
        this.id = kind.getId();
        this.name = kind.getName();
        this.label = kind.getLabel();
        this.updatedAt = kind.getUpdatedAt();
    }
}
