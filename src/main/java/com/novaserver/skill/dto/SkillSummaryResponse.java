package com.novaserver.skill.dto;

import com.novaserver.skill.entity.Skill;

/** 웹용 스킬 목록 응답. */
public record SkillSummaryResponse(Long id, String name, String castType, String projectilePath) {

    public static SkillSummaryResponse from(Skill skill) {
        return new SkillSummaryResponse(
                skill.getId(),
                skill.getName(),
                skill.getCastType().getName(),
                skill.getProjectilePath().getName());
    }
}
