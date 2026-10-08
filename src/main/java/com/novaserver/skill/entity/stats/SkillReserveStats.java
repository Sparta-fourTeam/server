package com.novaserver.skill.entity.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 예비 공격 수치. */
@Embeddable
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillReserveStats {
    @Column(name = "reserve_enabled")
    private boolean enabled;

    @Column(name = "reserve_distance")
    private Float distance;

    @Column(name = "reserve_cooldown")
    private Float cooldown;

    @Column(name = "reserve_interval")
    private Float interval;

    /**
     * 예비 공격 수치를 생성한다.
     *
     * @param enabled 그룹 사용 여부
     * @param distance 거리
     * @param cooldown 쿨다운
     * @param interval 간격
     */
    public SkillReserveStats(boolean enabled, Float distance, Float cooldown, Float interval) {
        this.enabled = enabled;
        this.distance = distance;
        this.cooldown = cooldown;
        this.interval = interval;
    }
}
