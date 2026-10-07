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
    @Column(name = "chain_bounces")
    private Float bounces;

    @Column(name = "chain_jump_range")
    private Float jumpRange;

    @Column(name = "chain_hop_interval")
    private Float hopInterval;

    @Column(name = "chain_path_width")
    private Float pathWidth;
}
