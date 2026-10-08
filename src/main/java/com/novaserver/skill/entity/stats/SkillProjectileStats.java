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
    @Column(name = "projectile_enabled")
    private boolean enabled;

    @Column(name = "projectile_speed")
    private Float speed;

    @Column(name = "projectile_pierce_count")
    private Integer pierceCount;

    @Column(name = "projectile_knockback_distance")
    private Float knockbackDistance;

    /**
     * 투사체 수치를 생성한다.
     *
     * @param enabled 그룹 사용 여부
     * @param speed 속도
     * @param pierceCount 관통 횟수
     * @param knockbackDistance 넉백 거리
     */
    public SkillProjectileStats(
            boolean enabled, Float speed, Integer pierceCount, Float knockbackDistance) {
        this.enabled = enabled;
        this.speed = speed;
        this.pierceCount = pierceCount;
        this.knockbackDistance = knockbackDistance;
    }
}
