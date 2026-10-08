package com.novaserver.battle.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.novaserver.battle.entity.BattleSession;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BattleRepositoryTest {

    @Autowired private BattleRepository battleRepository;

    @Test
    void createsNewRowOnFirstUpsert() {
        int affected = battleRepository.upsert(1L, "battle-1", LocalDateTime.now());

        assertThat(affected).isGreaterThan(0);
        assertThat(battleRepository.findById(1L))
                .get()
                .extracting(BattleSession::getBattleId)
                .isEqualTo("battle-1");
    }

    @Test
    void overwritesBattleIdWhenRowExists() {
        battleRepository.upsert(1L, "battle-1", LocalDateTime.now());

        battleRepository.upsert(1L, "battle-2", LocalDateTime.now());

        assertThat(battleRepository.findAll()).hasSize(1);
        assertThat(battleRepository.findById(1L))
                .get()
                .extracting(BattleSession::getBattleId)
                .isEqualTo("battle-2");
    }

    @Test
    void deletesWhenUserAndBattleIdMatch() {
        battleRepository.upsert(1L, "battle-1", LocalDateTime.now());

        int deleted = battleRepository.deleteByUserIdAndBattleId(1L, "battle-1");

        assertThat(deleted).isEqualTo(1);
        assertThat(battleRepository.existsById(1L)).isFalse();
    }

    @Test
    void doesNotDeleteWhenBattleIdMismatches() {
        battleRepository.upsert(1L, "battle-1", LocalDateTime.now());

        int deleted = battleRepository.deleteByUserIdAndBattleId(1L, "wrong-id");

        assertThat(deleted).isEqualTo(0);
        assertThat(battleRepository.existsById(1L)).isTrue();
    }
}
