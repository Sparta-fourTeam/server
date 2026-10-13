package com.novaserver.skill.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.cards.dto.CardDataResponse;
import com.novaserver.skill.entity.Skill;
import java.util.List;

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
        Integer unlockLevel,
        List<CardDataResponse> upgrades) {

    /** 스킬 및 소속 강화 카드. 유니티용 데이터 응답을 만든다. */
    public static SkillDataResponse from(Skill skill, List<CardDataResponse> upgrades) {
        return new SkillDataResponse(
                skill.getId(),
                skill.getName(),
                skill.getDesc(),
                skill.getCastType().getName(),
                skill.getProjectilePath().getName(),
                skill.getMaxLevel(),
                skill.isChildOnly(),
                SkillBaseStatsResponse.from(skill.getBaseStats()),
                skill.getUnlockLevel(),
                upgrades);
    }
}
