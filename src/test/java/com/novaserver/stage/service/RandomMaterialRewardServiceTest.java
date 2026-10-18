package com.novaserver.stage.service;

import static com.novaserver.support.RewardRulesFixture.EQUIPMENT;
import static com.novaserver.support.RewardRulesFixture.RANDOM_EQUIPMENT;
import static com.novaserver.support.RewardRulesFixture.RANDOM_SKILL;
import static com.novaserver.support.RewardRulesFixture.SKILL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.skill.entity.Skill;
import com.novaserver.skill.repository.SkillRepository;
import com.novaserver.stage.dto.ClearRewardResult;
import com.novaserver.stage.dto.RewardItemRule;
import com.novaserver.stage.dto.RewardRulesData;
import com.novaserver.support.RewardRulesFixture;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.upgrade.repository.UpgradeRepository;
import com.novaserver.user.entity.UserData;
import com.novaserver.user.repository.UserDataRepository;
import com.novaserver.user.service.PlayerLevelPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RandomMaterialRewardServiceTest {
    @Mock private ItemRepository items;
    @Mock private SkillRepository skills;
    @Mock private UpgradeRepository upgrades;
    @Mock private UserDataRepository users;
    @Mock private PlayerLevelPolicy levels;
    @InjectMocks private RandomMaterialRewardService resolver;
    private RewardRulesData rules;

    @BeforeEach
    void setUp() {
        rules = RewardRulesFixture.rules();
        lenient().when(users.findByUserId(1L)).thenReturn(Optional.of(new UserData(null, 0)));
        lenient().when(levels.levelAt(0)).thenReturn(1);
        lenient()
                .when(items.findAllById(any()))
                .thenAnswer(
                        call -> {
                            List<String> ids = call.getArgument(0);
                            if (ids == null) {
                                return List.of();
                            }
                            return RewardRulesFixture.catalog().stream()
                                    .filter(item -> ids.contains(item.getItemId()))
                                    .toList();
                        });
        Skill skill = mock(Skill.class);
        lenient().when(skill.getUnlockLevel()).thenReturn(1);
        lenient().when(skill.getId()).thenReturn(1L);
        lenient().when(skills.findAllById(any())).thenReturn(List.of(skill));
        Upgrade equipment = mock(Upgrade.class);
        lenient().when(equipment.getUnlockLevel()).thenReturn(1);
        lenient().when(equipment.getUpgradeId()).thenReturn("fixture.equipment.target");
        lenient().when(upgrades.findAllById(any())).thenReturn(List.of(equipment));
    }

    @Test
    void combinesFixedAndRepeatedRandomPicksAndResolvesBothKinds() {
        var result =
                resolver.resolve(
                        1L,
                        new ClearRewardResult(
                                30, 5, Map.of(SKILL, 7, RANDOM_SKILL, 9, RANDOM_EQUIPMENT, 4), 17),
                        rules);
        assertThat(result.items())
                .containsExactlyInAnyOrderEntriesOf(Map.of(SKILL, 16, EQUIPMENT, 4));
        assertThat(result.exp()).isEqualTo(30);
        assertThat(result.threeStarBonusExp()).isEqualTo(17);
    }

    @ParameterizedTest
    @CsvSource({"1,1,0", "4,2,2", "5,3,2", "2147483647,1073741824,1073741823"})
    void splitsUpToTwoPicksAndExcludesLockedSkills(int quantity, int first, int second) {
        var mapping = new HashMap<>(rules.items());
        mapping.put("fixture.skill.second", new RewardItemRule("SkillMaterial", false));
        mapping.put("fixture.skill.locked", new RewardItemRule("SkillMaterial", false));
        rules = new RewardRulesData(rules.schedules(), mapping);
        when(items.findAllById(any()))
                .thenReturn(
                        List.of(
                                new Item(SKILL, "1"),
                                new Item("fixture.skill.second", "2"),
                                new Item("fixture.skill.locked", "3")));
        Skill unlocked = mock(Skill.class);
        when(unlocked.getUnlockLevel()).thenReturn(1);
        when(unlocked.getId()).thenReturn(2L);
        Skill locked = mock(Skill.class);
        when(locked.getUnlockLevel()).thenReturn(3);
        when(locked.getId()).thenReturn(3L);
        Skill firstSkill = mock(Skill.class);
        when(firstSkill.getId()).thenReturn(1L);
        when(firstSkill.getUnlockLevel()).thenReturn(1);
        when(skills.findAllById(any())).thenReturn(List.of(firstSkill, unlocked, locked));
        RandomGenerator random = mock(RandomGenerator.class);
        when(random.nextInt(2)).thenReturn(0, 1);
        var result =
                resolver.resolve(
                        1L,
                        new ClearRewardResult(0, 0, Map.of(RANDOM_SKILL, quantity), 0),
                        rules,
                        random);
        assertThat(result.items().get(SKILL)).isEqualTo(first);
        assertThat(result.items().getOrDefault("fixture.skill.second", 0)).isEqualTo(second);
        assertThat(result.items()).doesNotContainKeys("fixture.skill.locked", RANDOM_SKILL);
    }

    @Test
    void rejectsNoUnlockedCandidatesInvalidTargetAndMergedOverflow() {
        when(levels.levelAt(0)).thenReturn(0);
        var reward = new ClearRewardResult(0, 0, Map.of(RANDOM_EQUIPMENT, 3), 0);
        assertThatThrownBy(() -> resolver.resolve(1L, reward, rules))
                .isInstanceOf(ResponseStatusException.class);
        when(levels.levelAt(0)).thenReturn(1);
        when(items.findAllById(any())).thenReturn(List.of(new Item(EQUIPMENT, null)));
        assertThatThrownBy(() -> resolver.resolve(1L, reward, rules))
                .isInstanceOf(ResponseStatusException.class);
        when(items.findAllById(any())).thenReturn(RewardRulesFixture.catalog());
        assertThatThrownBy(
                        () ->
                                resolver.resolve(
                                        1L,
                                        new ClearRewardResult(
                                                0,
                                                0,
                                                Map.of(SKILL, Integer.MAX_VALUE, RANDOM_SKILL, 1),
                                                0),
                                        rules))
                .isInstanceOf(ResponseStatusException.class);
    }
}
