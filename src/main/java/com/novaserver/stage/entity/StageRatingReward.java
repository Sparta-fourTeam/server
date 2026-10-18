package com.novaserver.stage.entity;

import com.novaserver.reward.entity.RewardDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 별등급 최초 클리어 보상. stageId와 rating(1~3) 조합으로 하나씩 존재한다. */
@Getter
@Entity
@Table(
        name = "stage_rating_reward",
        uniqueConstraints = @UniqueConstraint(columnNames = {"stage_id", "rating"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StageRatingReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @Column(nullable = false)
    private int rating;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reward_definition_id", nullable = false)
    private RewardDefinition rewardDefinition;

    /** 연결한 고정 보상 묶음의 골드를 읽는다. */
    public int getGold() {
        return rewardDefinition.getGold();
    }

    /** 연결한 고정 보상 묶음의 실제 아이템별 수량을 읽는다. */
    public Map<String, Integer> getItems() {
        return rewardDefinition.itemAmounts();
    }

    /** 고정 보상 묶음을 참조하는 레이팅을 만든다. */
    public StageRatingReward(Stage stage, int rating, RewardDefinition definition) {
        this.stage = stage;
        this.rating = rating;
        this.rewardDefinition = definition;
    }

    /** 공통 보상 묶음을 연결한다. 과거 영수증은 변경하지 않는다. */
    public void linkDefinition(RewardDefinition definition) {
        this.rewardDefinition = definition;
    }
}
