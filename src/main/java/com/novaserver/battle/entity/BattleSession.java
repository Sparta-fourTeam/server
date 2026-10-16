package com.novaserver.battle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저별 진행 중인 배틀 세션. 유저당 하나만 존재한다. */
@Entity
@Table(name = "battle_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleSession {
    @Id private Long userId;

    @Column(nullable = false, unique = true)
    private String battleId;

    @Column(nullable = false, unique = true)
    private String createId;

    private LocalDateTime createdAt;

    /**
     * 배틀 세션을 생성한다. 생성 시각은 현재 시각으로 기록한다.
     *
     * @param userId 유저 ID
     * @param battleId 발급한 배틀 ID
     * @param createId 발급 요청에 사용된 멱등성 키
     */
    public BattleSession(Long userId, String battleId, String createId) {
        this.userId = userId;
        this.battleId = battleId;
        this.createId = createId;
        this.createdAt = LocalDateTime.now();
    }
}
