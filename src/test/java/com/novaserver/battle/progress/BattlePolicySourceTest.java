package com.novaserver.battle.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class BattlePolicySourceTest {
    @Test
    void publishedInputsGenerateWaveBoundsWithReleaseScaleAndDescendants() throws Exception {
        var source =
                new BattlePolicySource(
                        BattleSourceFixture.stages(),
                        BattleSourceFixture.monsters(),
                        BattleSourceFixture.combat());
        assertThat(source.manifest().stageIds()).containsExactly(1);
        assertThat(source.manifest().maxTimeScale()).isEqualByComparingTo("1.0");
        assertThat(source.policy(1).waves()).hasSize(20);
        assertThat(source.policy(1).descendants()).hasSize(3);
        assertThat(source.policy(1).descendants().get(1L))
                .containsExactly(new StageProgressPolicy.SpawnLimit(3L, 2));
        assertThat(source.revision()).matches("[0-9a-f]{64}");
    }

    @Test
    void anySourceChangeChangesWholeSnapshotRevisionAndUnknownSpawnFailsStartup() throws Exception {
        String stages = raw("Stages");
        String monsters = raw("Monsters");
        String combat = raw("Combat");
        var source = new BattlePolicySource(stages, monsters, combat);
        var json = new ObjectMapper();
        var rows = json.readTree(stages);
        ((ObjectNode) rows.get(0).get("Spawn")).put("Cooldown", 7);
        assertThat(new BattlePolicySource(rows.toString(), monsters, combat).revision())
                .isNotEqualTo(source.revision());
        ((ObjectNode) rows.get(0).get("Waves").get(0).get("Spawns").get(0))
                .put("MonsterId", 999999);
        assertThatThrownBy(() -> new BattlePolicySource(rows.toString(), monsters, combat))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String raw(String name) {
        return switch (name) {
            case "Stages" -> BattleSourceFixture.stages();
            case "Monsters" -> BattleSourceFixture.monsters();
            case "Combat" -> BattleSourceFixture.combat();
            default -> throw new IllegalArgumentException(name);
        };
    }
}
