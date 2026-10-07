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
    @Column(name = "explosion_radius")
    private Float radius;

    @Column(name = "explosion_damage_ratio")
    private Float damageRatio;
}
