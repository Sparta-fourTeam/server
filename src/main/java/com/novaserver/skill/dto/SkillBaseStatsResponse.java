package com.novaserver.skill.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.skill.entity.SkillBaseStats;
import com.novaserver.skill.entity.stats.*;

/** 스킬 기본 수치. 스킬이 사용하지 않는 그룹은 null로 두고 응답에서 제외한다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SkillBaseStatsResponse(
        Cast cast,
        Projectile projectile,
        Reserve reserve,
        Status status,
        Explosion explosion,
        Area area,
        Chain chain,
        Beam beam,
        Field field) {

    public static SkillBaseStatsResponse from(SkillBaseStats baseStats) {
        if (baseStats == null) {
            return null;
        }
        return new SkillBaseStatsResponse(
                baseStats.getCast() == null ? null : Cast.from(baseStats.getCast()),
                baseStats.getProjectile() == null
                        ? null
                        : Projectile.from(baseStats.getProjectile()),
                baseStats.getReserve() == null ? null : Reserve.from(baseStats.getReserve()),
                baseStats.getStatus() == null ? null : Status.from(baseStats.getStatus()),
                baseStats.getExplosion() == null ? null : Explosion.from(baseStats.getExplosion()),
                baseStats.getArea() == null ? null : Area.from(baseStats.getArea()),
                baseStats.getChain() == null ? null : Chain.from(baseStats.getChain()),
                baseStats.getBeam() == null ? null : Beam.from(baseStats.getBeam()),
                baseStats.getField() == null ? null : Field.from(baseStats.getField()));
    }

    /** 시전 수치. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Cast(
            Float cooldown,
            Float baseDamage,
            Float range,
            Integer projectileCount,
            Integer castCount,
            Float castInterval) {
        static Cast from(SkillCastStats s) {
            return new Cast(
                    s.getCooldown(),
                    s.getBaseDamage(),
                    s.getRange(),
                    s.getProjectileCount(),
                    s.getCastCount(),
                    s.getCastInterval());
        }
    }

    /** 투사체 수치. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Projectile(Float speed, Integer pierceCount, Float knockbackDistance) {
        static Projectile from(SkillProjectileStats s) {
            return new Projectile(s.getSpeed(), s.getPierceCount(), s.getKnockbackDistance());
        }
    }

    /** 예약 시전 수치. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Reserve(Float distance, Float cooldown, Float interval) {
        static Reserve from(SkillReserveStats s) {
            return new Reserve(s.getDistance(), s.getCooldown(), s.getInterval());
        }
    }

    /** 상태이상 수치. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Status(
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
        static Status from(SkillStatusStats s) {
            return new Status(
                    s.getFreezeDuration(),
                    s.getFreezeChance(),
                    s.getFrostbiteChance(),
                    s.getParalysisDuration(),
                    s.getParalysisChance(),
                    s.getStunDuration(),
                    s.getStunChance(),
                    s.getSlowDuration(),
                    s.getSlowRatio(),
                    s.getBurnChance());
        }
    }

    /** 폭발 수치. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Explosion(Float radius, Float damageRatio) {
        static Explosion from(SkillExplosionStats s) {
            return new Explosion(s.getRadius(), s.getDamageRatio());
        }
    }

    /** 장판 수치. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Area(
            Float radius, Float duration, Float pulseInterval, Float moveSpeed, Float pull) {
        static Area from(SkillAreaStats s) {
            return new Area(
                    s.getRadius(),
                    s.getDuration(),
                    s.getPulseInterval(),
                    s.getMoveSpeed(),
                    s.getPull());
        }
    }

    /** 연쇄 수치. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Chain(Integer bounces, Float jumpRange, Float hopInterval, Float pathWidth) {
        static Chain from(SkillChainStats s) {
            return new Chain(
                    s.getBounces(), s.getJumpRange(), s.getHopInterval(), s.getPathWidth());
        }
    }

    /** 빔 수치. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Beam(Float length, Float width, Float duration, Integer pulses) {
        static Beam from(SkillBeamStats s) {
            return new Beam(s.getLength(), s.getWidth(), s.getDuration(), s.getPulses());
        }
    }

    /** 필드 수치. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Field(Float damageRatio, Float radius, Float slowRatio) {
        static Field from(SkillFieldStats s) {
            return new Field(s.getDamageRatio(), s.getRadius(), s.getSlowRatio());
        }
    }
}
