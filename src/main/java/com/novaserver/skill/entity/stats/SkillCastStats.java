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
}
