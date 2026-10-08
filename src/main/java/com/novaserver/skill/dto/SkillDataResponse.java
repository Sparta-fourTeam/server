package com.novaserver.skill.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.skill.entity.Skill;

/** 웹용 스킬 목록 응답. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SkillDataResponse(
        Long id,
        String name,
        String desc,
        String castType,
        String projectilePath,
        Integer maxLevel,
        Boolean childOnly,
        SkillBaseStatsResponse baseStats,
        Integer unlockLevel) {

    public static SkillDataResponse from(Skill skill) {
        return new SkillDataResponse(
                skill.getId(),
                skill.getName(),
                skill.getDesc(),
                skill.getCastType().getName(),
                skill.getProjectilePath() == null ? null : skill.getProjectilePath().getName(),
                skill.getMaxLevel(),
                skill.isChildOnly(),
                SkillBaseStatsResponse.from(skill.getBaseStats()),
                skill.getUnlockLevel());
    }
}
