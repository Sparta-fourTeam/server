package com.novaserver.skill.dto;

import com.novaserver.skill.entity.Skill;
import java.util.List;
import java.util.Map;

/** 웹용 스킬 목록 응답. */
public record SkillSummaryResponse(
        Long id,
        String name,
        String castType,
        String projectilePath,
        String element,
        List<String> statGroups) {

    /** 스킬 엔티티로부터 목록용 요약 응답을 만든다. */
    public static SkillSummaryResponse from(Skill skill) {
        return new SkillSummaryResponse(
                skill.getId(),
                skill.getName(),
                skill.getCastType().getName(),
                skill.getProjectilePath().getName(),
                skill.getElement() != null ? skill.getElement().getName() : null,
                statGroupsOf(skill.getBaseStats()));
    }

    /** 스킬이 사용하는 기본 수치 그룹 이름 목록. 프론트 필터용. */
    private static List<String> statGroupsOf(Map<String, Map<String, Double>> baseStats) {
        return baseStats == null ? List.of() : List.copyOf(baseStats.keySet());
    }
}
