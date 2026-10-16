package com.novaserver.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.battle.dto.BattleEndRequest;
import com.novaserver.battle.dto.BattleResponse;
import com.novaserver.battle.entity.BattleSession;
import com.novaserver.battle.repository.BattleRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.stage.dto.ClearRewardResult;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.service.StageRewardService;
import com.novaserver.stage.service.StageService;
import com.novaserver.user.entity.UserStageProgress;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserStageProgressRepository;
import com.novaserver.user.service.UserRewardService;
import com.novaserver.user.service.UserService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BattleServiceTest {

    @Mock private BattleRepository battleRepository;
    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private UserRewardService userRewardService;
    @Mock private UserStageProgressRepository userStageProgressRepository;
    @Mock private StageRewardService stageRewardService;
    @Mock private StageService stageService;

    @InjectMocks private BattleService battleService;

    private static BattleEndRequest endRequest(String battleId, int clearedWave, int rating) {
        BattleEndRequest request = new BattleEndRequest();
        ReflectionTestUtils.setField(request, "battleId", battleId);
        ReflectionTestUtils.setField(request, "stageId", 1);
        ReflectionTestUtils.setField(request, "clearedWave", clearedWave);
        ReflectionTestUtils.setField(request, "rating", rating);
        return request;
    }

    @Test
    void issuesAndSavesBattleId() {
        when(battleRepository.findById(1L)).thenReturn(Optional.empty());

        BattleResponse response = battleService.createBattle(1L, "key-1");

        assertThat(response.getBattleId()).isNotBlank();
        verify(battleRepository).upsert(eq(1L), eq(response.getBattleId()), eq("key-1"), any());
    }

    @Test
    void consumesEnergyWhenCreatingBattle() {
        when(battleRepository.findById(1L)).thenReturn(Optional.empty());

        battleService.createBattle(1L, "key-1");

        verify(userService).useEnergy(1L, 5);
    }

    @Test
    void doesNotIssueBattleIdWhenEnergyInsufficient() {
        when(battleRepository.findById(1L)).thenReturn(Optional.empty());
        doThrow(ErrorCode.INSUFFICIENT_ENERGY.exception()).when(userService).useEnergy(1L, 5);

        assertThatThrownBy(() -> battleService.createBattle(1L, "key-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("에너지가 부족합니다");
        verify(battleRepository, never()).upsert(any(), any(), any(), any());
    }

    @Test
    void returnsExistingBattleIdWhenCreateIdRepeats() {
        BattleSession existing = new BattleSession(1L, "battle-1", "key-1");
        when(battleRepository.findById(1L)).thenReturn(Optional.of(existing));

        BattleResponse response = battleService.createBattle(1L, "key-1");

        assertThat(response.getBattleId()).isEqualTo("battle-1");
        verify(userService, never()).useEnergy(any(), anyInt());
        verify(battleRepository, never()).upsert(any(), any(), any(), any());
    }

    @Test
    void issuesNewBattleIdWhenCreateIdDiffersFromExisting() {
        BattleSession existing = new BattleSession(1L, "battle-1", "key-1");
        when(battleRepository.findById(1L)).thenReturn(Optional.of(existing));

        BattleResponse response = battleService.createBattle(1L, "key-2");

        assertThat(response.getBattleId()).isNotEqualTo("battle-1");
        verify(userService).useEnergy(1L, 5);
        verify(battleRepository).upsert(eq(1L), eq(response.getBattleId()), eq("key-2"), any());
    }

    @Test
    void grantsRewardAndDeletesWhenUserAndBattleIdMatch() {
        BattleSession existing = new BattleSession(1L, "battle-1", "key-1");
        when(battleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(battleRepository.deleteByUserIdAndBattleId(1L, "battle-1")).thenReturn(1);
        when(stageRewardService.calculateClearReward(1, 10, 3))
                .thenReturn(new ClearRewardResult(100, 50, Map.of("mat.wood", 5)));
        Stage stage = mock(Stage.class);
        when(stageService.findStage(1)).thenReturn(stage);
        when(userStageProgressRepository.findByUserIdAndStageId(1L, 1))
                .thenReturn(Optional.empty());
        UserStageProgress progress = new UserStageProgress(null, stage);
        when(userStageProgressRepository.save(any())).thenReturn(progress);

        battleService.endBattle(1L, endRequest("battle-1", 10, 3));

        verify(userRewardService).grant(1L, 100, 50, Map.of("mat.wood", 5));
        assertThat(progress.getClearRating()).isEqualTo(3);
        verify(battleRepository).deleteByUserIdAndBattleId(1L, "battle-1");
    }

    @Test
    void throwsNotFoundWhenNoBattleInProgress() {
        when(battleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> battleService.endBattle(1L, endRequest("battle-1", 10, 3)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("진행 중인 배틀이 없습니다");
    }

    @Test
    void throwsBadRequestWhenBattleIdMismatches() {
        BattleSession existing = new BattleSession(1L, "battle-1", "key-1");
        when(battleRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> battleService.endBattle(1L, endRequest("wrong-id", 10, 3)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("배틀 ID가 일치하지 않습니다");
    }
}
