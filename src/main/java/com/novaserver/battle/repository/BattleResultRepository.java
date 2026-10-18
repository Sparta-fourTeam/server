package com.novaserver.battle.repository;

import com.novaserver.battle.entity.BattleResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 계정별 전투 확정 결과 저장소. */
public interface BattleResultRepository extends JpaRepository<BattleResult, String> {
    List<BattleResult> findAllByUserId(Long userId);

    Optional<BattleResult> findByUserIdAndBattleId(Long userId, String battleId);
}
