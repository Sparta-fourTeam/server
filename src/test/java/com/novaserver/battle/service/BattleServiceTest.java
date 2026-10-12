package com.novaserver.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.battle.dto.BattleResponse;
import com.novaserver.battle.repository.BattleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BattleServiceTest {

    @Mock private BattleRepository battleRepository;

    @InjectMocks private BattleService battleService;

    @Test
    void issuesAndSavesBattleId() {
        BattleResponse response = battleService.createBattle(1L);

        assertThat(response.getBattleId()).isNotBlank();
        verify(battleRepository).upsert(eq(1L), eq(response.getBattleId()), any());
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
