package com.novaserver.cards.dto;

import com.novaserver.cards.entity.SkillForm;
import java.time.LocalDateTime;
import lombok.Getter;

/** 스킬 형태 응답. */
@Getter
public class SkillFormResponse {
    private final Long id;
    private final String name;
    private final String label;
    private final LocalDateTime updatedAt;

    /** 엔티티로부터 응답을 만든다. */
    public SkillFormResponse(SkillForm form) {
        this.id = form.getId();
        this.name = form.getName();
        this.label = form.getLabel();
        this.updatedAt = form.getUpdatedAt();
    }
}
