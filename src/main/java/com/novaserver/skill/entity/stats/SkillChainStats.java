package com.novaserver.skill.entity.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 연쇄 공격 수치. */
@Embeddable
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillChainStats {
    @Column(name = "chain_enabled")
    private boolean enabled;

    @Column(name = "chain_bounces")
    private Integer bounces;

    @Column(name = "chain_jump_range")
    private Float jumpRange;

    @Column(name = "chain_hop_interval")
    private Float hopInterval;

    @Column(name = "chain_path_width")
    private Float pathWidth;

    /**
     * 연쇄 공격 수치를 생성한다.
     *
     * @param enabled 그룹 사용 여부
     * @param bounces 튕기는 횟수
     * @param jumpRange 도약 사거리
     * @param hopInterval 도약 간격
     * @param pathWidth 경로 폭
     */
    public SkillChainStats(
            boolean enabled, Integer bounces, Float jumpRange, Float hopInterval, Float pathWidth) {
        this.enabled = enabled;
        this.bounces = bounces;
        this.jumpRange = jumpRange;
        this.hopInterval = hopInterval;
        this.pathWidth = pathWidth;
    }
}
