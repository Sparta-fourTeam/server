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

    /**
     * 기본 전투 수치 묶음을 생성한다. 스킬이 사용하지 않는 그룹은 null로 둔다.
     *
     * @param cast 시전 수치
     * @param projectile 투사체 수치
     * @param reserve 예비 공격 수치
     * @param status 상태 이상 수치
     * @param explosion 명중 폭발 수치
     * @param area 영역 공격 수치
     * @param chain 연쇄 공격 수치
     * @param beam 광선 수치
     * @param field 전자기장 수치
     */
    public SkillBaseStats(
            SkillCastStats cast,
            SkillProjectileStats projectile,
            SkillReserveStats reserve,
            SkillStatusStats status,
            SkillExplosionStats explosion,
            SkillAreaStats area,
            SkillChainStats chain,
            SkillBeamStats beam,
            SkillFieldStats field) {
        this.cast = cast;
        this.projectile = projectile;
        this.reserve = reserve;
        this.status = status;
        this.explosion = explosion;
        this.area = area;
        this.chain = chain;
        this.beam = beam;
        this.field = field;
    }
}
