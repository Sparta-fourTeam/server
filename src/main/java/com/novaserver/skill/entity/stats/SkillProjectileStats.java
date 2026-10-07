package com.novaserver.skill.entity.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 투사체 수치. */
@Embeddable
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillProjectileStats {
    @Column(name = "projectile_speed")
    private Float speed;

    @Column(name = "projectile_pierce_count")
    private Integer pierceCount;

    @Column(name = "projectile_knockback_distance")
    private Float knockbackDistance;
}
