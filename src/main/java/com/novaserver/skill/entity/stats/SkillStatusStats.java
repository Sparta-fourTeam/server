package com.novaserver.skill.entity.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 상태 이상 수치. */
@Embeddable
@Getter
@NoArgsConstructor
public class SkillStatusStats {
    @Column(name = "status_enabled")
    private boolean enabled;

    // 빙결
    @Column(name = "status_freeze_duration")
    private Float freezeDuration;

    @Column(name = "status_freeze_chance")
    private Float freezeChance;

    // 동상
    @Column(name = "status_frostbite_chance")
    private Float frostbiteChance;

    // 마비
    @Column(name = "status_paralysis_duration")
    private Float paralysisDuration;

    @Column(name = "status_paralysis_chance")
    private Float paralysisChance;

    // 기절
    @Column(name = "status_stun_duration")
    private Float stunDuration;

    @Column(name = "status_stun_chance")
    private Float stunChance;

    // 감속
    @Column(name = "status_slow_duration")
    private Float slowDuration;

    @Column(name = "status_slow_ratio")
    private Float slowRatio;

    // 점화
    @Column(name = "status_burn_chance")
    private Float burnChance;

    /**
     * 상태 이상 수치를 생성한다.
     *
     * @param enabled 그룹 사용 여부
     * @param freezeDuration 빙결 지속시간
     * @param freezeChance 빙결 확률
     * @param frostbiteChance 동상 확률
     * @param paralysisDuration 마비 지속시간
     * @param paralysisChance 마비 확률
     * @param stunDuration 기절 지속시간
     * @param stunChance 기절 확률
     * @param slowDuration 감속 지속시간
     * @param slowRatio 감속 비율
     * @param burnChance 점화 확률
     */
    public SkillStatusStats(
            boolean enabled,
            Float freezeDuration,
            Float freezeChance,
            Float frostbiteChance,
            Float paralysisDuration,
            Float paralysisChance,
            Float stunDuration,
            Float stunChance,
            Float slowDuration,
            Float slowRatio,
            Float burnChance) {
        this.enabled = enabled;
        this.freezeDuration = freezeDuration;
        this.freezeChance = freezeChance;
        this.frostbiteChance = frostbiteChance;
        this.paralysisDuration = paralysisDuration;
        this.paralysisChance = paralysisChance;
        this.stunDuration = stunDuration;
        this.stunChance = stunChance;
        this.slowDuration = slowDuration;
        this.slowRatio = slowRatio;
        this.burnChance = burnChance;
    }
}
