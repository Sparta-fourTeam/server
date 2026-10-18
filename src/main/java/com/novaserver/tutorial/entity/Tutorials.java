package com.novaserver.tutorial.entity;

import com.novaserver.reward.entity.RewardDefinition;
import com.novaserver.stage.entity.Stage;
import com.novaserver.upgrade.entity.Upgrade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 문자열 ID별 서버 검증 조건과 고정 보상 연결. 대사 본문은 저장하지 않는다. */
@Entity
@Table(name = "tutorials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tutorials {
    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Tutorials prerequisite;

    @Column(nullable = false)
    private int minLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stage startStage;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stage clearStage;

    @ManyToOne(fetch = FetchType.LAZY)
    private Upgrade requiredUpgrade;

    @Column(nullable = false)
    private int upgradeLevel;

    @Column(nullable = false)
    private int playerLevel;

    @Column(nullable = false)
    private int setLevel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private RewardDefinition rewardDefinition;

    /** 관리자 지정 문자열 ID로 조건 정의를 만든다. */
    public Tutorials(String id) {
        this.id = id;
    }

    /** 검증된 선행·시작·완료 조건 및 고정 보상 묶음을 교체한다. */
    public void configure(
            Tutorials prerequisite,
            int minLevel,
            Stage startStage,
            Stage clearStage,
            Upgrade requiredUpgrade,
            int upgradeLevel,
            int playerLevel,
            int setLevel,
            RewardDefinition rewardDefinition) {
        this.prerequisite = prerequisite;
        this.minLevel = minLevel;
        this.startStage = startStage;
        this.clearStage = clearStage;
        this.requiredUpgrade = requiredUpgrade;
        this.upgradeLevel = upgradeLevel;
        this.playerLevel = playerLevel;
        this.setLevel = setLevel;
        this.rewardDefinition = rewardDefinition;
    }
}
