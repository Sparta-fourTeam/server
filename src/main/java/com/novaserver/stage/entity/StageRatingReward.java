package com.novaserver.stage.entity;

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

    @Column(nullable = false)
    private int gold;

    /**
     * 별등급 보상을 생성한다.
     *
     * @param stage 대상 스테이지
     * @param rating 클리어 등급(1~3)
     * @param gold 최초 클리어 시 지급할 골드
     */
    public StageRatingReward(Stage stage, int rating, int gold) {
        this.stage = stage;
        this.rating = rating;
        this.gold = gold;
    }

    /**
     * 보상 내용을 수정한다.
     *
     * @param gold 최초 클리어 시 지급할 골드
     */
    public void update(int gold) {
        this.gold = gold;
    }
}
