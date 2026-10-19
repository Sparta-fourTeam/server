package com.novaserver.battle.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 원본 파일 복제 대신 독립 입력으로 게시와 검증 규칙을 검사한다. */
public final class BattleSourceFixture {
    private BattleSourceFixture() {}

    /** 서로 독립된 소환 대상과 세 등급을 구성한다. */
    public static String monsters() {
        var rows = new ArrayList<Map<String, Object>>();
        for (int id = 1; id <= 3; id++) {
            var row = new java.util.HashMap<String, Object>();
            row.put("Id", id);
            for (String key :
                    List.of(
                            "Hp",
                            "Damage",
                            "Speed",
                            "AttackInterval",
                            "AttackRange",
                            "ProjectileSpeed",
                            "BurstCount",
                            "BurstInterval")) {
                row.put(key, 1);
            }
            row.put("Grade", id == 3 ? "Boss" : id == 2 ? "Elite" : "Normal");
            row.put("Resists", Map.of());
            row.put("CastResists", Map.of());
            row.put("BlocksProjectile", false);
            row.put(
                    "Passives",
                    id == 1
                            ? List.of(
                                    Map.of(
                                            "Kind",
                                            "Spawn",
                                            "Trigger",
                                            "OnDeath",
                                            "MonsterId",
                                            3,
                                            "Count",
                                            2))
                            : List.of());
            rows.add(row);
        }
        return encode(rows);
    }

    /** 테스트 전용 스테이지의 스폰을 구성한다. */
    public static String stages() {
        return encode(
                List.of(
                        Map.of(
                                "Id",
                                1,
                                "Spawn",
                                Map.of("IntervalMin", 0.3, "IntervalMax", 0.3, "Cooldown", 0),
                                "Waves",
                                java.util.Collections.nCopies(
                                        20,
                                        Map.of(
                                                "Spawns",
                                                List.of(Map.of("MonsterId", 2, "Count", 2)))))));
    }

    /** 테스트 배속이다. */
    public static String combat() {
        return "{\"BaseTimeScale\":0.5}";
    }

    /** 최초 게시 또는 갱신 요청을 만든다. */
    public static BattleSourceAdminService.Publication publication(String expected) {
        return new BattleSourceAdminService.Publication(stages(), monsters(), combat(), expected);
    }

    private static String encode(Object value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (java.io.IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
