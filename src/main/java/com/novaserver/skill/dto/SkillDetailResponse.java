package com.novaserver.skill.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.skill.entity.Skill;
import java.util.List;
import java.util.Map;

/** 웹용 스킬 상세 응답. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SkillDetailResponse(
        Long id,
        String name,
        String desc,
        NamedRef castType,
        NamedRef projectilePath,
        NamedRef element,
        Integer maxLevel,
        Boolean childOnly,
        Map<String, Map<String, Double>> baseStats,
        Integer unlockLevel,
        String progressionId,
        List<MirroredCardResponse> mirroredCards) {

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
                skill.getElement() != null
                        ? new NamedRef(skill.getElement().getId(), skill.getElement().getName())
                        : null,
                skill.getMaxLevel(),
                skill.isChildOnly(),
                skill.getBaseStats(),
                skill.getUnlockLevel(),
                skill.getProgressionId(),
                skill.getMirroredCards().stream().map(MirroredCardResponse::from).toList());
    }
}
