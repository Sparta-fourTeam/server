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
    @Column(name = "beam_length")
    private Float length;

    @Column(name = "beam_width")
    private Float width;

    @Column(name = "beam_duration")
    private Float duration;

    @Column(name = "beam_pulses")
    private Float pulses;
}
