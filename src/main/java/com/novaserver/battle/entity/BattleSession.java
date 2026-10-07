package com.novaserver.battle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "battle_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/**
 * 배틀 세션을 생성한다. 생성 시각은 현재 시각으로 기록한다.
 *
 * @param userId 유저 ID
 * @param battleId 발급한 배틀 ID
 */
public class BattleSession {
    @Id private Long userId;

    @Column(nullable = false, unique = true)
    private String battleId;

    private LocalDateTime createdAt;

    public BattleSession(Long userId, String battleId) {
        this.userId = userId;
        this.battleId = battleId;
        this.createdAt = LocalDateTime.now();
    }
}
