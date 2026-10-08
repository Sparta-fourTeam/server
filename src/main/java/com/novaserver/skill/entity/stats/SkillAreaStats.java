package com.novaserver.skill.entity.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 영역 공격 수치. */
@Embeddable
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillAreaStats {
    @Column(name = "area_enabled")
    private boolean enabled;

    @Column(name = "area_radius")
    private Float radius;

    @Column(name = "area_duration")
    private Float duration;

    @Column(name = "area_pulse_interval")
    private Float pulseInterval;

    @Column(name = "area_move_speed")
    private Float moveSpeed;

    @Column(name = "area_pull")
    private Float pull;

    /**
     * 영역 공격 수치를 생성한다.
     *
     * @param enabled 그룹 사용 여부
     * @param radius 반경
     * @param duration 지속시간
     * @param pulseInterval 틱 간격
     * @param moveSpeed 이동 속도
     * @param pull 끌어당김 힘
     */
    public SkillAreaStats(
            boolean enabled,
            Float radius,
            Float duration,
            Float pulseInterval,
            Float moveSpeed,
            Float pull) {
        this.enabled = enabled;
        this.radius = radius;
        this.duration = duration;
        this.pulseInterval = pulseInterval;
        this.moveSpeed = moveSpeed;
        this.pull = pull;
    }
}
