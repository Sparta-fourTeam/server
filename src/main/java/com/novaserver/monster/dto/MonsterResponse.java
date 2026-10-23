package com.novaserver.monster.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.monster.entity.CastType;
import com.novaserver.monster.entity.Element;
import com.novaserver.monster.entity.EnemyType;
import com.novaserver.monster.entity.Monster;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** 몬스터 응답. 클라이언트 Monsters JSON과 같은 PascalCase이고, 빈 Resists·Passives는 뺀다. */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MonsterResponse(
        Long id,
        int hp,
        int damage,
        double speed,
        double attackInterval,
        double attackRange,
        double projectileSpeed,
        EnemyType grade,
        int burstCount,
        double burstInterval,
        Map<Element, Double> resists,
        Map<CastType, Double> castResists,
        boolean blocksProjectile,
        List<MonsterPassiveResponse> passives) {

    /**
     * 엔티티를 응답으로 바꾼다.
     *
     * @param monster 몬스터 엔티티
     * @return 몬스터 응답
     */
    public static MonsterResponse from(Monster monster) {
        return new MonsterResponse(
                monster.getId(),
                monster.getHp(),
                monster.getDamage(),
                monster.getSpeed(),
                monster.getAttackInterval(),
                monster.getAttackRange(),
                monster.getProjectileSpeed(),
                monster.getGrade(),
                monster.getBurstCount(),
                monster.getBurstInterval(),
                monster.getResists(),
                monster.getCastResists(),
                monster.isBlocksProjectile(),
                monster.getPassives().stream().map(MonsterPassiveResponse::from).toList());
    }
}
