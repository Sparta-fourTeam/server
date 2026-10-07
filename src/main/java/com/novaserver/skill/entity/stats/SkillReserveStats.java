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
    @Column(name = "reserve_distance")
    private Float distance;

    @Column(name = "reserve_cooldown")
    private Float cooldown;

    @Column(name = "reserve_interval")
    private Float interval;
}
