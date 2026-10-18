package com.novaserver.monster.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class MonsterCatalogTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void acceptsExactClientCatalogWithAllTwentyOneIdsAndThreeGrades() throws IOException {
        var catalog = new MonsterCatalog();
        JsonNode rows = mapper.readTree(catalog.json());

        assertThat(rows).hasSize(21);
        assertThat(rows.get(0).get("Id").intValue()).isEqualTo(1);
        assertThat(rows.get(20).get("Id").intValue()).isEqualTo(100);
        assertThat(rows.findValuesAsText("Grade")).contains("Normal", "Elite", "Boss");
    }

    @Test
    void rejectsInvalidCombatValuesAndUnknownPassiveKinds() throws IOException {
        var invalidHp = source();
        ((ObjectNode) invalidHp.get(0)).put("Hp", 0);
        assertThatThrownBy(() -> MonsterCatalog.validate(invalidHp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hp");

        var invalidSpeed = source();
        ((ObjectNode) invalidSpeed.get(0)).put("Speed", -0.1);
        assertThatThrownBy(() -> MonsterCatalog.validate(invalidSpeed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Speed");

        var invalidKind = source();
        ((ObjectNode) ((ArrayNode) invalidKind.get(0).get("Passives")).get(0))
                .put("Kind", "Unknown");
        assertThatThrownBy(() -> MonsterCatalog.validate(invalidKind))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kind");
    }

    @Test
    void rejectsUnknownResistanceAndBrokenSpawnReferences() throws IOException {
        var invalidResist = source();
        ((ObjectNode) invalidResist.get(0)).set("Resists", mapper.readTree("{\"Plasma\":0.5}"));
        assertThatThrownBy(() -> MonsterCatalog.validate(invalidResist))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plasma");

        var missingTarget = source();
        ((ObjectNode) ((ArrayNode) missingTarget.get(0).get("Passives")).get(0))
                .put("MonsterId", 999);
        assertThatThrownBy(() -> MonsterCatalog.validate(missingTarget))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소환 대상");

        var selfSpawn = source();
        ((ObjectNode) ((ArrayNode) selfSpawn.get(0).get("Passives")).get(0)).put("MonsterId", 1);
        assertThatThrownBy(() -> MonsterCatalog.validate(selfSpawn))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("자기 소환");

        var chainSpawn = source();
        ((ObjectNode) chainSpawn.get(2))
                .set(
                        "Passives",
                        mapper.readTree(
                                """
                                [{"Kind":"Spawn","Trigger":"OnDeath","MonsterId":2,"Count":1}]
                                """));
        assertThatThrownBy(() -> MonsterCatalog.validate(chainSpawn))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("연쇄 소환");
    }

    @Test
    void acceptsEveryClientPassiveKindAndRejectsInvalidProbability() throws IOException {
        var rows = source();
        ((ObjectNode) rows.get(1))
                .set(
                        "Passives",
                        mapper.readTree(
                                """
                                [
                                  {"Kind":"Immunity","Statuses":["Stun"]},
                                  {"Kind":"Modifier","Target":"BurnDuration","Multiplier":0.5},
                                  {"Kind":"Shield","Trigger":"OnHit","Hits":2,"Chance":0.4},
                                  {"Kind":"Evade","Chance":0.2},
                                  {"Kind":"LowHp","HpRatio":0.3,"Duration":2,"Invulnerable":true},
                                  {"Kind":"HealOnHit","HealRatio":0.1,"Chance":0.5},
                                  {"Kind":"SpeedBoost","Interval":6,"Duration":1,"SpeedMultiplier":2}
                                ]
                                """));
        MonsterCatalog.validate(rows);

        ((ObjectNode) ((ArrayNode) rows.get(1).get("Passives")).get(3)).put("Chance", 1.1);
        assertThatThrownBy(() -> MonsterCatalog.validate(rows))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chance");
    }

    private JsonNode source() throws IOException {
        return mapper.readTree(new MonsterCatalog().json());
    }
}
