package com.novaserver.skill.dto;

import com.novaserver.skill.entity.SkillProjectilePath;
import java.time.LocalDateTime;
import lombok.Getter;

/** 스킬 투사체 경로 응답. */
@Getter
public class SkillProjectilePathResponse {
    private final Long id;
    private final String name;
    private final String label;
    private final LocalDateTime updatedAt;

    /** 엔티티로부터 응답을 만든다. */
    public SkillProjectilePathResponse(SkillProjectilePath projectilePath) {
        this.id = projectilePath.getId();
        this.name = projectilePath.getName();
        this.label = projectilePath.getLabel();
        this.updatedAt = projectilePath.getUpdatedAt();
    }
}
