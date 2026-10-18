package com.novaserver.quest.entity;

import com.novaserver.reward.entity.RewardDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 수령 점수로 여는 상자와 공통 골드·아이템 및 별도 에너지·젬 보상. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestChest {
    @Id
    @Column(length = 64)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestPeriod period;

    @Column(nullable = false)
    private int points;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private RewardDefinition rewardDefinition;

    @Column(nullable = false)
    private int energy;

    @Column(nullable = false)
    private int gems;

    /** 검증된 상자 조건과 보상을 만든다. */
    public QuestChest(
            String id,
            QuestPeriod period,
            int points,
            RewardDefinition reward,
            int energy,
            int gems) {
        this.id = id;
        this.period = period;
        this.points = points;
        this.rewardDefinition = reward;
        this.energy = energy;
        this.gems = gems;
    }
}
