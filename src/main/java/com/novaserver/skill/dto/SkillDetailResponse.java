package com.novaserver.skill.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.skill.entity.Skill;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SkillDetailResponse(
        Long id,
        String name,
        String desc,
        NamedRef castType,
        NamedRef projectilePath,
        Integer maxLevel,
        Boolean childOnly,
        SkillBaseStatsResponse baseStats,
        Integer unlockLevel) {

    /** 연관 테이블 값. */
    public record NamedRef(Long id, String name) {}

    public static SkillDetailResponse from(Skill skill) {
        return new SkillDetailResponse(
                skill.getId(),
                skill.getName(),
                skill.getDesc(),
                new NamedRef(skill.getCastType().getId(), skill.getCastType().getName()),
                new NamedRef(
                        skill.getProjectilePath().getId(), skill.getProjectilePath().getName()),
                skill.getMaxLevel(),
                skill.isChildOnly(),
                SkillBaseStatsResponse.from(skill.getBaseStats()),
                skill.getUnlockLevel());
    }
}
