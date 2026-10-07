package com.novaserver.skill.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.skill.entity.stats.SkillAreaStats;
import com.novaserver.skill.entity.stats.SkillBeamStats;
import com.novaserver.skill.entity.stats.SkillCastStats;
import com.novaserver.skill.entity.stats.SkillChainStats;
import com.novaserver.skill.entity.stats.SkillExplosionStats;
import com.novaserver.skill.entity.stats.SkillFieldStats;
import com.novaserver.skill.entity.stats.SkillProjectileStats;
import com.novaserver.skill.entity.stats.SkillReserveStats;
import com.novaserver.skill.entity.stats.SkillStatusStats;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 스킬 기본 전투 수치 묶음. 그룹 값이 전부 없으면 그룹째 응답에서 빠진다. */
@Embeddable
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillBaseStats {
    @Valid @Embedded private SkillCastStats cast;

    @Valid @Embedded private SkillProjectileStats projectile;

    @Valid @Embedded private SkillReserveStats reserve;

    @Valid @Embedded private SkillStatusStats status;

    @Valid @Embedded private SkillExplosionStats explosion;

    @Valid @Embedded private SkillAreaStats area;

    @Valid @Embedded private SkillChainStats chain;

    @Valid @Embedded private SkillBeamStats beam;

    @Valid @Embedded private SkillFieldStats field;
}
