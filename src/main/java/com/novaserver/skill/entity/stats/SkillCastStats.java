package com.novaserver.skill.entity.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 시전 수치. */
@Embeddable
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillCastStats {
    @Column(name = "cast_enabled")
    private boolean enabled;

    @Column(name = "cast_cooldown")
    private Float cooldown;

    @Column(name = "cast_base_damage")
    private Float baseDamage;

    @Column(name = "cast_range")
    private Float range;

    @Column(name = "cast_projectile_count")
    private Integer projectileCount;

    @Column(name = "cast_count")
    private Integer castCount;

    @Column(name = "cast_interval")
    private Float castInterval;

    /**
     * 시전 수치를 생성한다. 그룹이 체크됐다면 값이 전부 비어 있어도 enabled를 true로 둬서, 저장 후에도 그룹 자체가 사라지지 않게 한다.
     *
     * @param enabled 그룹 사용 여부
     * @param cooldown 쿨다운
     * @param baseDamage 기본 피해량
     * @param range 사거리
     * @param projectileCount 투사체 개수
     * @param castCount 시전 횟수
     * @param castInterval 시전 간격
     */
    public SkillCastStats(
            boolean enabled,
            Float cooldown,
            Float baseDamage,
            Float range,
            Integer projectileCount,
            Integer castCount,
            Float castInterval) {
        this.enabled = enabled;
        this.cooldown = cooldown;
        this.baseDamage = baseDamage;
        this.range = range;
        this.projectileCount = projectileCount;
        this.castCount = castCount;
        this.castInterval = castInterval;
    }
}
