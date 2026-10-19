package com.novaserver.user.entity;

import com.novaserver.quest.entity.Quest;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저별 퀘스트 진행도. periodKey가 지난 주기 것이면 조회·갱신 시점에 리셋한다. */
@Entity
@Table(
        name = "user_quest_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quest_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserQuestProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @Column(nullable = false)
    private int count;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private boolean claimed;

    @Column(nullable = false)
    private LocalDate periodKey;

    /** 진행도 0, 미완료·미수령 상태로 만든다. */
    public UserQuestProgress(User user, Quest quest, LocalDate periodKey) {
        this.user = user;
        this.quest = quest;
        this.count = 0;
        this.completed = false;
        this.claimed = false;
        this.periodKey = periodKey;
    }

    /** 주기가 바뀌었으면(날짜가 지났으면) 진행도를 초기화한다. */
    public void resetIfStale(LocalDate currentPeriodKey) {
        if (!this.periodKey.equals(currentPeriodKey)) {
            this.count = 0;
            this.completed = false;
            this.claimed = false;
            this.periodKey = currentPeriodKey;
        }
    }

    /** 진행도를 target까지 올린다. 이미 완료했으면 그대로 둔다. */
    public void increase(int amount, int target) {
        if (completed) {
            return;
        }

        this.count = Math.min(target, this.count + amount);
        if (this.count >= target) {
            this.completed = true;
        }
    }

    /** 완료한 퀘스트의 보상을 수령 처리한다. 완료·미수령 여부는 호출하는 쪽에서 확인한다. */
    public void claim() {
        this.claimed = true;
    }
}
