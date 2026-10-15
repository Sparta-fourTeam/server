package com.novaserver.skill.dto;

import com.novaserver.skill.entity.SkillElement;
import java.time.LocalDateTime;
import lombok.Getter;

/** 속성 응답. */
@Getter
public class SkillElementResponse {
    private final Long id;
    private final String name;
    private final String label;
    private final LocalDateTime updatedAt;

    /** 엔티티로부터 응답을 만든다. */
    public SkillElementResponse(SkillElement element) {
        this.id = element.getId();
        this.name = element.getName();
        this.label = element.getLabel();
        this.updatedAt = element.getUpdatedAt();
    }
}
