package com.novaserver.quest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 퀘스트 누적 포인트 보상. 해당 주기의 포인트가 points 이상이면 수령할 수 있다. */
@Entity
@Table(
        name = "quest_reward",
        uniqueConstraints = @UniqueConstraint(columnNames = {"period", "points"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestReward {
    @Id private String rewardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestPeriod period;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false)
    private int gold;

    @Column(nullable = false)
    private int energy;

    /** 보상을 생성한다. */
    public QuestReward(String rewardId, QuestPeriod period, int points, int gold, int energy) {
        this.rewardId = rewardId;
        this.period = period;
        this.points = points;
        this.gold = gold;
        this.energy = energy;
    }

    /** 보상 내용을 수정한다. rewardId·period·points(포인트 구간)는 바꿀 수 없다. */
    public void update(int gold, int energy) {
        this.gold = gold;
        this.energy = energy;
    }
}
