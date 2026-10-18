package com.novaserver.stage.service;

import static com.novaserver.support.RewardRulesFixture.CHEST;
import static com.novaserver.support.RewardRulesFixture.EQUIPMENT;
import static com.novaserver.support.RewardRulesFixture.RANDOM_EQUIPMENT;
import static com.novaserver.support.RewardRulesFixture.RANDOM_SKILL;
import static com.novaserver.support.RewardRulesFixture.SKILL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.stage.dto.RewardItemRule;
import com.novaserver.stage.dto.RewardRulesData;
import com.novaserver.stage.dto.StageRewardItemRequest;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.entity.StageReward;
import com.novaserver.stage.entity.StageRewardItem;
import com.novaserver.stage.repository.StageRewardRepository;
import com.novaserver.support.RewardRulesFixture;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StageRewardServiceTest {
    private static final int BASE_EXP = 13;
    private static final int BONUS_EXP = 17;
    private static final int FINAL_COIN = 67;
    private static final int SKILL_FINAL = 83;
    private static final int EQUIPMENT_FINAL = 26;
    private static final int CHEST_FINAL = 49;
    @Mock private StageRewardRepository stageRewardRepository;
    @Mock private StageService stageService;
    @Mock private ItemRepository itemRepository;
    @Mock private StageRewardItemValidator itemValidator;
    @Mock private RewardRulesService rulesService;
    private StageRewardService rewards;
    private StageReward balance;

    @BeforeEach
    void setUp() {
        var rules = RewardRulesFixture.rules();
        lenient().when(rulesService.getRules()).thenReturn(rules);
        rewards =
                new StageRewardService(
                        stageRewardRepository,
                        stageService,
                        itemRepository,
                        itemValidator,
                        rulesService);
        balance =
                new StageReward(
                        Stage.create(1),
                        BASE_EXP,
                        BONUS_EXP,
                        FINAL_COIN,
                        List.of(
                                new StageRewardItem(SKILL, SKILL_FINAL),
                                new StageRewardItem(EQUIPMENT, EQUIPMENT_FINAL),
                                new StageRewardItem(CHEST, CHEST_FINAL)));
        lenient().when(stageRewardRepository.findById(1)).thenReturn(Optional.of(balance));
        // targetId와 무관하게 입력 데이터의 분류를 사용한다.
        lenient().when(itemRepository.findAllById(any())).thenReturn(RewardRulesFixture.catalog());
    }

    @ParameterizedTest
    @CsvSource({
        "0,3,0,2,1,2",
        "1,5,13,5,2,4",
        "3,9,13,11,4,8",
        "4,11,13,35,5,10",
        "5,13,13,38,6,12",
        "6,15,13,41,7,14",
        "7,41,13,44,8,16",
        "8,43,13,47,14,18",
        "9,45,13,50,15,27",
        "20,67,13,83,26,49"
    })
    void appliesSyntheticSchedulesAtWaveBoundaries(
            int wave, int gold, int exp, int skill, int equipment, int chest) {
        var result = rewards.calculateClearReward(1, wave, 0);
        assertThat(result.gold()).isEqualTo(gold);
        assertThat(result.exp()).isEqualTo(exp);
        assertThat(result.items()).containsEntry(SKILL, skill);
        assertThat(result.items().getOrDefault(EQUIPMENT, 0)).isEqualTo(equipment);
        assertThat(result.items().getOrDefault(CHEST, 0)).isEqualTo(chest);
    }

    @ParameterizedTest
    @CsvSource({"1,13", "2,13", "3,30"})
    void paysBaseExpOnceAndOnlyThreeStarsAddBonus(int rating, int exp) {
        var result = rewards.calculateClearReward(1, 20, rating);
        assertThat(result.exp()).isEqualTo(exp);
        assertThat(result.gold()).isEqualTo(FINAL_COIN);
        assertThat(result.items())
                .containsEntry(SKILL, SKILL_FINAL)
                .containsEntry(EQUIPMENT, EQUIPMENT_FINAL)
                .containsEntry(CHEST, CHEST_FINAL);
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "21,0", "0,-1", "20,4", "0,1", "19,3"})
    void rejectsInconsistentOutcome(int wave, int rating) {
        assertThatThrownBy(() -> rewards.calculateClearReward(1, wave, rating))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("완료 웨이브와 별 등급");
    }

    @Test
    void expOverflowCannotWrapIntoAnUnpaidOrSmallerReward() {
        balance.update(Integer.MAX_VALUE, 1, FINAL_COIN, balance.getItems());
        assertThatThrownBy(() -> rewards.calculateClearReward(1, 20, 3))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("전투 보상 기준 데이터");
    }

    @ParameterizedTest
    @ValueSource(strings = {RANDOM_SKILL, RANDOM_EQUIPMENT, "unknown.reward"})
    void unresolvedOrUnknownRewardCannotBeStoredAsAnInventoryPlaceholder(String itemId) {
        balance.update(BASE_EXP, BONUS_EXP, FINAL_COIN, List.of(new StageRewardItem(itemId, 41)));
        when(itemRepository.findAllById(any())).thenReturn(List.of(new Item(itemId, null)));
        assertThatThrownBy(() -> rewards.calculateClearReward(1, 20, 3))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("전투 보상 기준 데이터");
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(new Item(itemId, null)));
        assertThatThrownBy(
                        () ->
                                new StageRewardItemValidator(itemRepository, rulesService)
                                        .validate(List.of(item(itemId, 41))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("지급 규칙이 준비되지 않은");
    }

    @Test
    void adminValidationUsesTheSameScheduleAndMinimumAsCalculation() {
        when(itemRepository.findById(SKILL)).thenReturn(Optional.of(new Item(SKILL, null)));
        when(itemRepository.findById(EQUIPMENT))
                .thenReturn(Optional.of(new Item(EQUIPMENT, "fixture.equipment.target")));
        var validator = new StageRewardItemValidator(itemRepository, rulesService);
        assertThat(validator.validate(List.of(item(SKILL, 62), item(EQUIPMENT, 21)))).hasSize(2);
        assertThatThrownBy(() -> validator.validate(List.of(item(SKILL, 61))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("아이템");
        assertThatThrownBy(
                        () -> validator.validate(List.of(item(EQUIPMENT, 26), item(EQUIPMENT, 27))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("중복");
    }

    @Test
    void newItemClassificationComesFromDataRatherThanIdOrTarget() {
        var old = rulesService.getRules();
        var mapping = new HashMap<>(old.items());
        mapping.put("custom.material", new RewardItemRule("EquipmentMaterial", false));
        when(rulesService.getRules()).thenReturn(new RewardRulesData(old.schedules(), mapping));
        when(itemRepository.findAllById(any()))
                .thenReturn(List.of(new Item("custom.material", "1")));
        balance.update(
                BASE_EXP,
                BONUS_EXP,
                FINAL_COIN,
                List.of(new StageRewardItem("custom.material", 29)));
        assertThat(rewards.calculateClearReward(1, 7, 0).items())
                .containsEntry("custom.material", 8);
        assertThat(rewards.calculateClearReward(1, 8, 0).items())
                .containsEntry("custom.material", 17);
        var deferredSchedules = new HashMap<>(old.schedules());
        deferredSchedules.put("EquipmentMaterial", new StageRewardSchedule(0, 0, 8));
        when(rulesService.getRules()).thenReturn(new RewardRulesData(deferredSchedules, mapping));
        assertThat(rewards.calculateClearReward(1, 7, 0).items()).isEmpty();
        assertThat(rewards.calculateClearReward(1, 8, 0).items())
                .containsEntry("custom.material", 29);
    }

    private StageRewardItemRequest item(String id, int amount) {
        var request = new StageRewardItemRequest();
        ReflectionTestUtils.setField(request, "itemId", id);
        ReflectionTestUtils.setField(request, "finalAmount", amount);
        return request;
    }
}
