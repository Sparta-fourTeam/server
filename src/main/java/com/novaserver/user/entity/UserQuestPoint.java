package com.novaserver.user.entity;

import com.novaserver.quest.entity.QuestPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저별 주기(일일·주간) 누적 퀘스트 포인트와 보상 수령 현황. */
@Entity
@Table(
        name = "user_quest_point",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "period"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserQuestPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestPeriod period;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false)
    private int claimedPoints;

    @Column(nullable = false)
    private LocalDate periodKey;

    /** 포인트 0, 수령 포인트 0 상태로 만든다. */
    public UserQuestPoint(User user, QuestPeriod period, LocalDate periodKey) {
        this.user = user;
        this.period = period;
        this.points = 0;
        this.claimedPoints = 0;
        this.periodKey = periodKey;
    }

    /** 주기가 바뀌었으면(날짜가 지났으면) 포인트를 초기화한다. */
    public void resetIfStale(LocalDate currentPeriodKey) {
        if (!this.periodKey.equals(currentPeriodKey)) {
            this.points = 0;
            this.claimedPoints = 0;
            this.periodKey = currentPeriodKey;
        }
    }

    /** 완료한 퀘스트의 points를 더한다. */
    public void addPoints(int amount) {
        this.points += amount;
    }

    /** 기존보다 높을 때만 수령 포인트를 갱신한다. */
    public void updateClaimedPoints(int claimedPoints) {
        if (claimedPoints > this.claimedPoints) {
            this.claimedPoints = claimedPoints;
        }
    }
}
