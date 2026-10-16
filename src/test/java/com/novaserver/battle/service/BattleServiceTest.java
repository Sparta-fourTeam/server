package com.novaserver.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.battle.dto.BattleResponse;
import com.novaserver.battle.entity.BattleSession;
import com.novaserver.battle.repository.BattleRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BattleServiceTest {

    @Mock private BattleRepository battleRepository;
    @Mock private UserService userService;

    @InjectMocks private BattleService battleService;

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
    void deletesWhenUserAndBattleIdMatch() {
        when(battleRepository.deleteByUserIdAndBattleId(1L, "battle-1")).thenReturn(1);

        battleService.endBattle(1L, "battle-1");

        verify(battleRepository).deleteByUserIdAndBattleId(1L, "battle-1");
    }

    @Test
    void throwsNotFoundWhenNoBattleInProgress() {
        when(battleRepository.deleteByUserIdAndBattleId(eq(1L), anyString())).thenReturn(0);
        when(battleRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> battleService.endBattle(1L, "battle-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("진행 중인 배틀이 없습니다");
    }

    @Test
    void throwsBadRequestWhenBattleIdMismatches() {
        when(battleRepository.deleteByUserIdAndBattleId(eq(1L), anyString())).thenReturn(0);
        when(battleRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> battleService.endBattle(1L, "wrong-id"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("배틀 ID가 일치하지 않습니다");
    }
}
