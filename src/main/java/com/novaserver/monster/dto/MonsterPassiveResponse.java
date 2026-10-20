package com.novaserver.monster.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.monster.entity.Element;
import com.novaserver.monster.entity.MonsterPassive;
import com.novaserver.monster.entity.PassiveKind;
import com.novaserver.monster.entity.PassiveTrigger;
import com.novaserver.monster.entity.StatusImmunity;
import com.novaserver.monster.entity.Target;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** 몬스터 패시브 응답. 그 Kind가 쓰지 않는 값은 빼서 클라이언트 Monsters JSON과 같은 모양으로 내보낸다. */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MonsterPassiveResponse(
        PassiveKind kind,
        PassiveTrigger trigger,
        Long monsterId,
        Integer count,
        Double interval,
        Integer maxTotal,
        List<StatusImmunity> statuses,
        Target target,
        Double multiplier,
        Integer hits,
        Double duration,
        Double chance,
        Double hpRatio,
        Double healRatio,
        Boolean invulnerable,
        Double speedMultiplier,
        Element element) {

    /**
     * 엔티티를 응답으로 바꾼다.
     *
     * @param passive 패시브 엔티티
     * @return 패시브 응답
     */
    public static MonsterPassiveResponse from(MonsterPassive passive) {
        return new MonsterPassiveResponse(
                passive.getKind(),
                passive.getTrigger(),
                passive.getSpawnMonster() != null ? passive.getSpawnMonster().getId() : null,
                passive.getCount(),
                passive.getInterval(),
                passive.getMaxTotal(),
                passive.getStatuses().stream().sorted().toList(),
                passive.getTarget(),
                passive.getMultiplier(),
                passive.getHits(),
                passive.getDuration(),
                passive.getChance(),
                passive.getHpRatio(),
                passive.getHealRatio(),
                passive.getInvulnerable(),
                passive.getSpeedMultiplier(),
                passive.getElement());
    }
}
