package com.novaserver.monster.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.monster.dto.MonsterPassiveRequest;
import com.novaserver.monster.entity.Element;
import com.novaserver.monster.entity.Monster;
import com.novaserver.monster.entity.MonsterPassive;
import com.novaserver.monster.entity.PassiveTrigger;
import com.novaserver.monster.entity.StatusImmunity;
import com.novaserver.monster.entity.Target;
import com.novaserver.monster.repository.MonsterRepository;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 패시브 요청을 Kind별 규칙으로 검증하고, 그 Kind가 쓰는 값만 담은 패시브를 만든다. */
@Component
@RequiredArgsConstructor
public class MonsterPassiveFactory {
    private final MonsterRepository monsterRepository;

    /**
     * Kind에 맞는 패시브를 만든다.
     *
     * @param passive 패시브 요청
     * @param monster 패시브를 가질 몬스터
     * @return 검증된 패시브
     */
    public MonsterPassive create(MonsterPassiveRequest passive, Monster monster) {
        return switch (passive.getKind()) {
            case Spawn -> createSpawnPassive(passive, monster);
            case Immunity -> createImmunityPassive(passive);
            case Modifier -> createModifierPassive(passive);
            case Shield -> createShieldPassive(passive);
            case Evade -> createEvadePassive(passive);
            case LowHp -> createLowHpPassive(passive);
            case HealOnHit -> createHealOnHitPassive(passive);
            case SpeedBoost -> createSpeedBoostPassive(passive);
        };
    }

    private MonsterPassive createSpawnPassive(MonsterPassiveRequest passive, Monster monster) {
        PassiveTrigger trigger = require(passive.getTrigger(), "Spawn.Trigger");
        check(
                trigger.getCategory() == PassiveTrigger.Category.Spawn,
                "Spawn.Trigger는 OnDeath 또는 Interval이어야 합니다");

        Long spawnedId = require(passive.getMonsterId(), "Spawn.MonsterId");
        check(!Objects.equals(spawnedId, monster.getId()), "자기 자신은 소환할 수 없습니다: " + spawnedId);

        Monster spawned =
                monsterRepository
                        .findById(spawnedId)
                        .orElseThrow(() -> ErrorCode.MONSTER_NOT_FOUND.exception(spawnedId));
        check(!spawned.hasSpawnPassive(), "소환 대상이 Spawn 패시브를 가지고 있습니다(연쇄 소환): " + spawnedId);

        int count = requireAtLeastOne(passive.getCount(), "Spawn.Count");

        Double interval = null;
        Integer maxTotal = null;
        if (trigger == PassiveTrigger.Interval) {
            interval = requirePositive(passive.getInterval(), "Spawn.Interval");
            maxTotal = requireAtLeastOne(passive.getMaxTotal(), "Spawn.MaxTotal");
        }

        return MonsterPassive.builder()
                .kind(passive.getKind())
                .trigger(trigger)
                .spawnMonster(spawned)
                .count(count)
                .interval(interval)
                .maxTotal(maxTotal)
                .build();
    }

    private MonsterPassive createImmunityPassive(MonsterPassiveRequest passive) {
        Set<StatusImmunity> statuses = require(passive.getStatuses(), "Immunity.Statuses");
        check(!statuses.isEmpty(), "Immunity.Statuses는 1개 이상이어야 합니다");

        return MonsterPassive.builder().kind(passive.getKind()).statuses(statuses).build();
    }

    private MonsterPassive createModifierPassive(MonsterPassiveRequest passive) {
        Target target = require(passive.getTarget(), "Modifier.Target");
        double multiplier = requireNonNegative(passive.getMultiplier(), "Modifier.Multiplier");

        return MonsterPassive.builder()
                .kind(passive.getKind())
                .target(target)
                .multiplier(multiplier)
                .build();
    }

    private MonsterPassive createShieldPassive(MonsterPassiveRequest passive) {
        PassiveTrigger trigger = require(passive.getTrigger(), "Shield.Trigger");
        check(
                trigger.getCategory() == PassiveTrigger.Category.Shield,
                "Shield.Trigger는 Start 또는 OnHit이어야 합니다");

        int hits = requireAtLeastOne(passive.getHits(), "Shield.Hits");

        // 생략하거나 0이면 횟수를 다 쓸 때까지 유지된다.
        Double duration = passive.getDuration();
        if (duration != null) {
            requireNonNegative(duration, "Shield.Duration");
        }

        Double chance = null;
        if (trigger == PassiveTrigger.OnHit) {
            chance = requireChance(passive.getChance(), "Shield.Chance");
        }

        // 선택값이다. 21번처럼 함께 막을 상태이상이 있을 때만 들어온다.
        Set<StatusImmunity> statuses = passive.getStatuses();

        return MonsterPassive.builder()
                .kind(passive.getKind())
                .trigger(trigger)
                .hits(hits)
                .duration(duration)
                .chance(chance)
                .statuses(statuses)
                .build();
    }

    private MonsterPassive createEvadePassive(MonsterPassiveRequest passive) {
        double chance = requireChance(passive.getChance(), "Evade.Chance");

        return MonsterPassive.builder().kind(passive.getKind()).chance(chance).build();
    }

    private MonsterPassive createLowHpPassive(MonsterPassiveRequest passive) {
        double duration = requirePositive(passive.getDuration(), "LowHp.Duration");

        double hpRatio = require(passive.getHpRatio(), "LowHp.HpRatio");
        check(hpRatio > 0 && hpRatio < 1, "LowHp.HpRatio는 0 초과 1 미만이어야 합니다");

        Double healRatio = passive.getHealRatio();
        boolean heals = healRatio != null && healRatio > 0;
        boolean invulnerable = Boolean.TRUE.equals(passive.getInvulnerable());
        Double speedMultiplier = passive.getSpeedMultiplier();
        boolean changesSpeed = speedMultiplier != null && speedMultiplier != 1;
        check(heals || invulnerable || changesSpeed, "LowHp는 회복·무적·이동 속도 중 하나 이상의 효과가 필요합니다");

        return MonsterPassive.builder()
                .kind(passive.getKind())
                .duration(duration)
                .hpRatio(hpRatio)
                .healRatio(healRatio)
                .invulnerable(invulnerable)
                .speedMultiplier(speedMultiplier)
                .build();
    }

    private MonsterPassive createHealOnHitPassive(MonsterPassiveRequest passive) {
        // 생략하면 클라이언트가 1(항상 발동)로 처리한다.
        Double chance = passive.getChance();
        if (chance != null) {
            requireChance(chance, "HealOnHit.Chance");
        }

        Double healRatio = passive.getHealRatio();
        if (healRatio != null) {
            requireNonNegative(healRatio, "HealOnHit.HealRatio");
        }

        Double speedMultiplier = passive.getSpeedMultiplier();
        if (speedMultiplier != null) {
            requirePositive(speedMultiplier, "HealOnHit.SpeedMultiplier");
        }

        boolean heals = healRatio != null && healRatio > 0;
        boolean changesSpeed = speedMultiplier != null && speedMultiplier != 1;
        check(heals || changesSpeed, "HealOnHit는 회복·이동 속도 중 하나 이상의 효과가 필요합니다");

        Double duration = null;
        if (changesSpeed) {
            duration = requirePositive(passive.getDuration(), "HealOnHit.Duration");
        }

        // 생략하면 모든 속성의 피해에 반응한다.
        Element element = passive.getElement();

        return MonsterPassive.builder()
                .kind(passive.getKind())
                .duration(duration)
                .chance(chance)
                .healRatio(healRatio)
                .speedMultiplier(speedMultiplier)
                .element(element)
                .build();
    }

    private MonsterPassive createSpeedBoostPassive(MonsterPassiveRequest passive) {
        double interval = requirePositive(passive.getInterval(), "SpeedBoost.Interval");
        double duration = requirePositive(passive.getDuration(), "SpeedBoost.Duration");

        double speedMultiplier =
                require(passive.getSpeedMultiplier(), "SpeedBoost.SpeedMultiplier");
        check(speedMultiplier > 1, "SpeedBoost.SpeedMultiplier는 1보다 커야 합니다");

        return MonsterPassive.builder()
                .kind(passive.getKind())
                .interval(interval)
                .duration(duration)
                .speedMultiplier(speedMultiplier)
                .build();
    }

    private static <T> T require(T value, String field) {
        check(value != null, field + "는 필수입니다");
        return value;
    }

    private static int requireAtLeastOne(Integer value, String field) {
        int required = require(value, field);
        check(required >= 1, field + "는 1 이상이어야 합니다");
        return required;
    }

    private static double requirePositive(Double value, String field) {
        double required = require(value, field);
        check(required > 0, field + "는 0보다 커야 합니다");
        return required;
    }

    private static double requireNonNegative(Double value, String field) {
        double required = require(value, field);
        check(required >= 0, field + "는 0 이상이어야 합니다");
        return required;
    }

    private static double requireChance(Double value, String field) {
        double required = require(value, field);
        check(required > 0 && required <= 1, field + "는 0 초과 1 이하여야 합니다");
        return required;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw ErrorCode.MONSTER_INVALID.exception(message);
        }
    }
}
