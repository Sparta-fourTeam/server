package com.novaserver.skill.dto;

import com.novaserver.skill.entity.SkillBaseStats;
import com.novaserver.skill.entity.stats.SkillAreaStats;
import com.novaserver.skill.entity.stats.SkillBeamStats;
import com.novaserver.skill.entity.stats.SkillCastStats;
import com.novaserver.skill.entity.stats.SkillChainStats;
import com.novaserver.skill.entity.stats.SkillExplosionStats;
import com.novaserver.skill.entity.stats.SkillFieldStats;
import com.novaserver.skill.entity.stats.SkillProjectileStats;
import com.novaserver.skill.entity.stats.SkillReserveStats;
import com.novaserver.skill.entity.stats.SkillStatusStats;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;

/** 스킬 기본 수치 등록·수정 요청. 스킬이 사용하지 않는 그룹은 null로 보낸다. */
@Getter
public class SkillBaseStatsRequest {
    @Valid private Cast cast;

    @Valid private Projectile projectile;

    @Valid private Reserve reserve;

    @Valid private Status status;

    @Valid private Explosion explosion;

    @Valid private Area area;

    @Valid private Chain chain;

    @Valid private Beam beam;

    @Valid private Field field;

    /** 요청 DTO를 임베디드 엔티티로 변환한다. */
    public SkillBaseStats toEntity() {
        return new SkillBaseStats(
                cast == null ? null : cast.toEntity(),
                projectile == null ? null : projectile.toEntity(),
                reserve == null ? null : reserve.toEntity(),
                status == null ? null : status.toEntity(),
                explosion == null ? null : explosion.toEntity(),
                area == null ? null : area.toEntity(),
                chain == null ? null : chain.toEntity(),
                beam == null ? null : beam.toEntity(),
                field == null ? null : field.toEntity());
    }

    /** 시전 수치. */
    @Getter
    public static class Cast {
        private Float cooldown;
        private Float baseDamage;
        private Float range;
        private Integer projectileCount;
        private Integer castCount;
        private Float castInterval;

        SkillCastStats toEntity() {
            return new SkillCastStats(
                    true, cooldown, baseDamage, range, projectileCount, castCount, castInterval);
        }
    }

    /** 투사체 수치. */
    @Getter
    public static class Projectile {
        private Float speed;
        private Integer pierceCount;
        private Float knockbackDistance;

        SkillProjectileStats toEntity() {
            return new SkillProjectileStats(true, speed, pierceCount, knockbackDistance);
        }
    }

    /** 예비 공격 수치. */
    @Getter
    public static class Reserve {
        private Float distance;
        private Float cooldown;
        private Float interval;

        SkillReserveStats toEntity() {
            return new SkillReserveStats(true, distance, cooldown, interval);
        }
    }

    /** 상태 이상 수치. */
    @Getter
    public static class Status {
        private Float freezeDuration;

        @DecimalMin(value = "0", message = "빙결 확률은 0 이상입니다")
        @DecimalMax(value = "1", message = "빙결 확률은 1 이하입니다")
        private Float freezeChance;

        @DecimalMin(value = "0", message = "동상 확률은 0 이상입니다")
        @DecimalMax(value = "1", message = "동상 확률은 1 이하입니다")
        private Float frostbiteChance;

        private Float paralysisDuration;

        @DecimalMin(value = "0", message = "마비 확률은 0 이상입니다")
        @DecimalMax(value = "1", message = "마비 확률은 1 이하입니다")
        private Float paralysisChance;

        private Float stunDuration;

        @DecimalMin(value = "0", message = "기절 확률은 0 이상입니다")
        @DecimalMax(value = "1", message = "기절 확률은 1 이하입니다")
        private Float stunChance;

        private Float slowDuration;

        @DecimalMin(value = "0", message = "감속 비율은 0 이상입니다")
        @DecimalMax(value = "1", message = "감속 비율은 1 이하입니다")
        private Float slowRatio;

        @DecimalMin(value = "0", message = "점화 확률은 0 이상입니다")
        @DecimalMax(value = "1", message = "점화 확률은 1 이하입니다")
        private Float burnChance;

        SkillStatusStats toEntity() {
            return new SkillStatusStats(
                    true,
                    freezeDuration,
                    freezeChance,
                    frostbiteChance,
                    paralysisDuration,
                    paralysisChance,
                    stunDuration,
                    stunChance,
                    slowDuration,
                    slowRatio,
                    burnChance);
        }
    }

    /** 명중 폭발 수치. */
    @Getter
    public static class Explosion {
        private Float radius;
        private Float damageRatio;

        SkillExplosionStats toEntity() {
            return new SkillExplosionStats(true, radius, damageRatio);
        }
    }

    /** 영역 공격 수치. */
    @Getter
    public static class Area {
        private Float radius;
        private Float duration;
        private Float pulseInterval;
        private Float moveSpeed;
        private Float pull;

        SkillAreaStats toEntity() {
            return new SkillAreaStats(true, radius, duration, pulseInterval, moveSpeed, pull);
        }
    }

    /** 연쇄 공격 수치. */
    @Getter
    public static class Chain {
        private Integer bounces;
        private Float jumpRange;
        private Float hopInterval;
        private Float pathWidth;

        SkillChainStats toEntity() {
            return new SkillChainStats(true, bounces, jumpRange, hopInterval, pathWidth);
        }
    }

    /** 광선 수치. */
    @Getter
    public static class Beam {
        private Float length;
        private Float width;
        private Float duration;
        private Integer pulses;

        SkillBeamStats toEntity() {
            return new SkillBeamStats(true, length, width, duration, pulses);
        }
    }

    /** 전자기장 수치. */
    @Getter
    public static class Field {
        private Float damageRatio;
        private Float radius;

        @DecimalMin(value = "0", message = "전자기장 감속 비율은 0 이상입니다")
        @DecimalMax(value = "1", message = "전자기장 감속 비율은 1 이하입니다")
        private Float slowRatio;

        SkillFieldStats toEntity() {
            return new SkillFieldStats(true, damageRatio, radius, slowRatio);
        }
    }
}
