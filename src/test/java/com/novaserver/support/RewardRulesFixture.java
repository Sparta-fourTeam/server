package com.novaserver.support;

import com.novaserver.item.entity.Item;
import com.novaserver.stage.dto.RewardItemRule;
import com.novaserver.stage.dto.RewardRulesData;
import com.novaserver.stage.service.StageRewardSchedule;
import java.util.List;
import java.util.Map;

/** 운영 밸런스와 독립된 계산·API 테스트 입력. 호출마다 새 데이터를 만든다. */
public final class RewardRulesFixture {
    public static final String SKILL = "fixture.skill";
    public static final String EQUIPMENT = "fixture.equipment";
    public static final String CHEST = "fixture.chest";
    public static final String RANDOM_SKILL = "fixture.random.skill";
    public static final String RANDOM_EQUIPMENT = "fixture.random.equipment";

    private RewardRulesFixture() {}

    /** 모든 종류의 시작량·증가량·서로 다른 보너스 경계를 가진 합성 일정. */
    public static RewardRulesData rules() {
        return new RewardRulesData(
                Map.of(
                        "Coin",
                        new StageRewardSchedule(3, 2, 7),
                        "SkillMaterial",
                        new StageRewardSchedule(2, 3, 4),
                        "EquipmentMaterial",
                        new StageRewardSchedule(1, 1, 8),
                        "GemChest",
                        new StageRewardSchedule(2, 2, 9)),
                Map.of(
                        SKILL,
                        new RewardItemRule("SkillMaterial", false),
                        EQUIPMENT,
                        new RewardItemRule("EquipmentMaterial", false),
                        CHEST,
                        new RewardItemRule("GemChest", false),
                        RANDOM_SKILL,
                        new RewardItemRule("SkillMaterial", true),
                        RANDOM_EQUIPMENT,
                        new RewardItemRule("EquipmentMaterial", true)));
    }

    /** 아이템 대상과 보상 분류가 독립임을 확인할 수 있는 테스트 카탈로그. */
    public static List<Item> catalog() {
        return List.of(
                new Item(SKILL, "1"),
                new Item(EQUIPMENT, "fixture.equipment.target"),
                new Item(CHEST, null));
    }
}
