package com.novaserver.skill.entity.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 광선 수치. */
@Embeddable
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillBeamStats {
    @Column(name = "beam_enabled")
    private boolean enabled;

    @Column(name = "beam_length")
    private Float length;

    @Column(name = "beam_width")
    private Float width;

    @Column(name = "beam_duration")
    private Float duration;

    @Column(name = "beam_pulses")
    private Integer pulses;

    /**
     * 광선 수치를 생성한다.
     *
     * @param enabled 그룹 사용 여부
     * @param length 길이
     * @param width 폭
     * @param duration 지속시간
     * @param pulses 틱 횟수
     */
    public SkillBeamStats(
            boolean enabled, Float length, Float width, Float duration, Integer pulses) {
        this.enabled = enabled;
        this.length = length;
        this.width = width;
        this.duration = duration;
        this.pulses = pulses;
    }
}
