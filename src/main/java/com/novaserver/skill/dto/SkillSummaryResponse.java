package com.novaserver.skill.dto;

import com.novaserver.skill.entity.Skill;
import com.novaserver.skill.entity.SkillBaseStats;
import java.util.ArrayList;
import java.util.List;

/** 웹용 스킬 목록 응답. */
public record SkillSummaryResponse(
        Long id, String name, String castType, String projectilePath, List<String> statGroups) {

    /** 스킬 엔티티로부터 목록용 요약 응답을 만든다. */
    public static SkillSummaryResponse from(Skill skill) {
        return new SkillSummaryResponse(
                skill.getId(),
                skill.getName(),
                skill.getCastType().getName(),
                skill.getProjectilePath().getName(),
                statGroupsOf(skill.getBaseStats()));
    }

    /** 스킬이 사용하는 기본 수치 그룹 이름 목록. 프론트 필터용. */
    private static List<String> statGroupsOf(SkillBaseStats stats) {
        List<String> groups = new ArrayList<>();
        if (stats == null) {
            return groups;
        }
        if (stats.getCast() != null) groups.add("cast");
        if (stats.getProjectile() != null) groups.add("projectile");
        if (stats.getReserve() != null) groups.add("reserve");
        if (stats.getStatus() != null) groups.add("status");
        if (stats.getExplosion() != null) groups.add("explosion");
        if (stats.getArea() != null) groups.add("area");
        if (stats.getChain() != null) groups.add("chain");
        if (stats.getBeam() != null) groups.add("beam");
        if (stats.getField() != null) groups.add("field");
        return groups;
    }
}
