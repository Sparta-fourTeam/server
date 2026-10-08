package com.novaserver.skill.entity.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 명중 폭발 수치. */
@Embeddable
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillExplosionStats {
    @Column(name = "explosion_enabled")
    private boolean enabled;

    @Column(name = "explosion_radius")
    private Float radius;

    @Column(name = "explosion_damage_ratio")
    private Float damageRatio;

    /**
     * 명중 폭발 수치를 생성한다.
     *
     * @param enabled 그룹 사용 여부
     * @param radius 폭발 반경
     * @param damageRatio 피해 비율
     */
    public SkillExplosionStats(boolean enabled, Float radius, Float damageRatio) {
        this.enabled = enabled;
        this.radius = radius;
        this.damageRatio = damageRatio;
    }
}
