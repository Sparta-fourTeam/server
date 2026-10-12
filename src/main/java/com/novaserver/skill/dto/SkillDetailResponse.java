package com.novaserver.skill.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.skill.entity.Skill;

/** 웹용 스킬 상세 응답. */
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

    /** 스킬 엔티티로부터 상세 응답을 만든다. */
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
