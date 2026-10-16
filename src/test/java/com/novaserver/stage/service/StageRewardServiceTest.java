package com.novaserver.stage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.stage.dto.ClearRewardResult;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.entity.StageReward;
import com.novaserver.stage.entity.StageRewardItem;
import com.novaserver.stage.repository.StageRewardRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StageRewardServiceTest {

    private static final int BASE_EXP = 10;
    private static final int THREE_STAR_BONUS_EXP = 50;
    private static final int FINAL_COIN = 125;
    private static final int SKILL_FINAL_AMOUNT = 21;
    private static final int EQUIPMENT_FINAL_AMOUNT = 7;

    @Mock private StageRewardRepository stageRewardRepository;
    @Mock private StageService stageService;
    @Mock private ItemRepository itemRepository;
    @Mock private StageRewardItemValidator itemValidator;

    private StageRewardService stageRewardService;

    @BeforeEach
    void setUp() {
        stageRewardService =
                new StageRewardService(
                        stageRewardRepository, stageService, itemRepository, itemValidator);

        Stage stage = Stage.create(1);
        StageReward reward =
                new StageReward(
                        stage,
                        BASE_EXP,
                        THREE_STAR_BONUS_EXP,
                        FINAL_COIN,
                        List.of(
                                new StageRewardItem("material.skill.fire", SKILL_FINAL_AMOUNT),
                                new StageRewardItem("mat.wood", EQUIPMENT_FINAL_AMOUNT)));
        when(stageRewardRepository.findById(1)).thenReturn(Optional.of(reward));

        Item skillMaterial = new Item("material.skill.fire", "skill-fire");
        Item equipmentMaterial = new Item("mat.wood", null);
        lenient()
                .when(itemRepository.findAllById(List.of("material.skill.fire", "mat.wood")))
                .thenReturn(List.of(skillMaterial, equipmentMaterial));
    }

    @Test
    void returnsNothingWhenNoWaveCleared() {
        ClearRewardResult result = stageRewardService.calculateClearReward(1, 0, 0);

        assertThat(result.exp()).isZero();
        assertThat(result.gold()).isZero();
        assertThat(result.items()).isEmpty();
    }

    @Test
    void givesBaseRewardOnlyOnWaveOne() {
        ClearRewardResult result = stageRewardService.calculateClearReward(1, 1, 0);

        assertThat(result.exp()).isEqualTo(BASE_EXP);
        assertThat(result.gold()).isEqualTo(25);
        assertThat(result.items()).containsEntry("material.skill.fire", 1);
        assertThat(result.items()).doesNotContainKey("mat.wood");
    }

    @Test
    void addsSkillMaterialBonusFromWaveThree() {
        ClearRewardResult result = stageRewardService.calculateClearReward(1, 3, 0);

        // 1 + 1*(3-1) + (21 - 1 - 1*20) = 3 + 0 = 3
        assertThat(result.items()).containsEntry("material.skill.fire", 3);
    }

    @Test
    void grantsEquipmentMaterialOnlyFromWaveFour() {
        ClearRewardResult beforeWaveFour = stageRewardService.calculateClearReward(1, 3, 0);
        assertThat(beforeWaveFour.items()).doesNotContainKey("mat.wood");

        ClearRewardResult atWaveFour = stageRewardService.calculateClearReward(1, 4, 0);
        assertThat(atWaveFour.items()).containsEntry("mat.wood", EQUIPMENT_FINAL_AMOUNT);
    }

    @Test
    void addsCoinBonusFromWaveSix() {
        ClearRewardResult beforeBonus = stageRewardService.calculateClearReward(1, 5, 0);
        assertThat(beforeBonus.gold()).isEqualTo(25 + 5 * 4);

        ClearRewardResult atBonusWave = stageRewardService.calculateClearReward(1, 6, 0);
        // 25 + 5*5 + (125 - 25 - 5*20) = 50 + 0 = 50
        assertThat(atBonusWave.gold()).isEqualTo(50);
    }

    @Test
    void addsThreeStarBonusExpOnlyAtRatingThree() {
        ClearRewardResult rating0 = stageRewardService.calculateClearReward(1, 20, 0);
        ClearRewardResult rating3 = stageRewardService.calculateClearReward(1, 20, 3);

        assertThat(rating0.exp()).isEqualTo(BASE_EXP * 20);
        assertThat(rating3.exp()).isEqualTo(BASE_EXP * 20 + THREE_STAR_BONUS_EXP);
    }

    @Test
    void clearingFinalWaveGivesNearlyFullCoinAndSkillMaterial() {
        ClearRewardResult result = stageRewardService.calculateClearReward(1, 20, 3);

        // 설계상 20웨이브 클리어 시 총합은 finalCoin/finalAmount에서 증가분 한 스텝만큼 모자르게 끝난다.
        assertThat(result.gold()).isEqualTo(FINAL_COIN - 5);
        assertThat(result.items()).containsEntry("material.skill.fire", SKILL_FINAL_AMOUNT - 1);
        assertThat(result.items()).containsEntry("mat.wood", EQUIPMENT_FINAL_AMOUNT);
    }
}
