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
}
