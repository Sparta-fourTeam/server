package com.novaserver.battle.repository;

import com.novaserver.battle.entity.BattleSession;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 배틀 세션 저장소. */
public interface BattleRepository extends JpaRepository<BattleSession, Long> {

    @Modifying
    @Query("delete from BattleSession b where b.userId = :userId and b.battleId = :battleId")
    int deleteByUserIdAndBattleId(@Param("userId") Long userId, @Param("battleId") String battleId);

    @Modifying
    @Query(
            value =
                    "INSERT INTO battle_session (user_id, battle_id, created_at) "
                            + "VALUES (:userId, :battleId, :createdAt) "
                            + "ON DUPLICATE KEY UPDATE battle_id = :battleId, "
                            + "created_at = :createdAt",
            nativeQuery = true)
    int upsert(
            @Param("userId") Long userId,
            @Param("battleId") String battleId,
            @Param("createdAt") LocalDateTime createdAt);
}
