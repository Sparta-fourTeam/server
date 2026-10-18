package com.novaserver.playerlevel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 플레이어 레벨 경험치 곡선. 단일 기준 행을 DB에서 관리한다. */
@Entity
@Table(name = "player_level_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerLevelConfig {
    public static final Long CONFIG_ID = 1L;
    // 클라이언트/API의 테이블 할당 크기 상한. 출시 경험치 밸런스와는 별개다.
    public static final int MAX_SUPPORTED_LEVEL = 1000;

    @Id private Long id = CONFIG_ID;

    @Column(nullable = false)
    private int baseExp;

    @Column(nullable = false)
    private int stepExp;

    @Column(nullable = false)
    private int maxLevel;

    /** 검증된 경험치 기준 데이터를 저장할 행을 만든다. */
    public PlayerLevelConfig(int baseExp, int stepExp, int maxLevel) {
        update(baseExp, stepExp, maxLevel);
    }

    /** 검증된 경험치 기준으로 교체한다. */
    public void update(int baseExp, int stepExp, int maxLevel) {
        this.baseExp = baseExp;
        this.stepExp = stepExp;
        this.maxLevel = maxLevel;
    }
}
