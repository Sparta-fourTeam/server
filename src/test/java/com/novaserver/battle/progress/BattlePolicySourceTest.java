package com.novaserver.battle.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.novaserver.monster.service.MonsterCatalog;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class BattlePolicySourceTest {
    @Test
    void actualResourcesGenerateAllFortyStagesWithReleaseScaleAndSpawnDescendants()
            throws Exception {
        var source = new BattlePolicySource(new MonsterCatalog());
        assertThat(source.manifest().stageIds())
                .containsExactlyElementsOf(
                        java.util.stream.IntStream.rangeClosed(1, 40).boxed().toList());
        assertThat(source.manifest().maxTimeScale()).isEqualByComparingTo("1.0");
        for (var stage : source.manifest().stageIds()) {
            var policy = source.policy(stage);
            assertThat(policy.waves()).hasSize(20);
            assertThat(policy.descendants()).hasSize(21);
        }
        assertThat(source.policy(1).minSpawnInterval()).isEqualByComparingTo("0.3");
        assertThat(source.policy(1).waves().getFirst())
                .containsExactlyEntriesOf(java.util.Map.of(2L, 2));
        assertThat(source.policy(1).descendants().get(1L))
                .containsExactly(new StageProgressPolicy.SpawnLimit(3L, 2));
        assertThat(source.policy(1).descendants().get(5L))
                .containsExactly(new StageProgressPolicy.SpawnLimit(6L, 4));
        assertThat(source.revision()).matches("[0-9a-f]{64}");
        assertThat(new BattlePolicySource(new MonsterCatalog()).revision())
                .isEqualTo(source.revision());
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

    private String raw(String name) throws Exception {
        try (var in = new ClassPathResource("game-data/" + name + ".json").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
