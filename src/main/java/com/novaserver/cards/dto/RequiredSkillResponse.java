package com.novaserver.cards.dto;

import com.novaserver.cards.entity.RequiredSkill;

/** 필요 스킬 조건 응답. */
public record RequiredSkillResponse(Long id, NamedRef skill) {

    /** 연관 테이블 값. */
    public record NamedRef(Long id, String name) {}

    /** 엔티티로부터 응답을 만든다. */
    public static RequiredSkillResponse from(RequiredSkill requiredSkill) {
        return new RequiredSkillResponse(
                requiredSkill.getId(),
                new NamedRef(requiredSkill.getSkill().getId(), requiredSkill.getSkill().getName()));
    }
}
