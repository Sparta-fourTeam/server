package com.novaserver.skill.dto;

import com.novaserver.skill.entity.SkillCastType;
import java.time.LocalDateTime;
import lombok.Getter;

/** 스킬 시전 방식 응답. */
@Getter
public class SkillCastTypeResponse {
    private final Long id;
    private final String name;
    private final String label;
    private final LocalDateTime updatedAt;

    public SkillCastTypeResponse(SkillCastType castType) {
        this.id = castType.getId();
        this.name = castType.getName();
        this.label = castType.getLabel();
        this.updatedAt = castType.getUpdatedAt();
    }
}
