package com.novaserver.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.Getter;

/** 스킬 등록·수정 요청. */
@Getter
public class SkillRequest {
    @NotBlank(message = "이름은 필수값입니다.")
    @Size(max = 255, message = "이름은 255자 이하입니다.")
    private String name;

    private String desc;

    @NotNull(message = "시전 방식은 필수값입니다.")
    private Long castTypeId;

    @NotNull(message = "투사체 경로는 필수값입니다.")
    private Long projectilePathId;

    private Long elementId;

    @NotNull(message = "최대 레벨은 필수값입니다.")
    @Positive(message = "최대 레벨은 1 이상입니다.")
    private Integer maxLevel;

    private boolean childOnly;

    /** 그룹별 기본 전투 수치. 예: {"cast": {"cooldown": 2.0, "baseDamage": 12}}. */
    private Map<String, Map<String, Double>> baseStats;

    private Integer unlockLevel;

    private String progressionId;
}
