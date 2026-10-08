package com.novaserver.skill.entity.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 전자기장 수치. */
@Embeddable
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillFieldStats {
    @Column(name = "field_enabled")
    private boolean enabled;

    @Column(name = "field_damage_ratio")
    private Float damageRatio;

    @Column(name = "field_radius")
    private Float radius;

    @DecimalMin(value = "0", message = "전자기장 감속 비율은 0 이상입니다.")
    @DecimalMax(value = "1", message = "전지기장 감속 비율은 1 이하입니다.")
    @Column(name = "field_slow_ratio")
    private Float slowRatio;

    /**
     * 전자기장 수치를 생성한다.
     *
     * @param enabled 그룹 사용 여부
     * @param damageRatio 피해 비율
     * @param radius 반경
     * @param slowRatio 감속 비율
     */
    public SkillFieldStats(boolean enabled, Float damageRatio, Float radius, Float slowRatio) {
        this.enabled = enabled;
        this.damageRatio = damageRatio;
        this.radius = radius;
        this.slowRatio = slowRatio;
    }
}
