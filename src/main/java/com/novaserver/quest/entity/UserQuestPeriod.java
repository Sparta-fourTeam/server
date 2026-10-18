package com.novaserver.quest.entity;

import com.novaserver.user.entity.User;
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
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 계정·주기별 서버 행동 집계. 과거 기간도 보존해 재전송이 새 기간에 영향을 주지 않는다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "period", "period_day"}))
public class UserQuestPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestPeriod period;

    @Column(name = "period_day", nullable = false)
    private LocalDate periodDay;

    @Column(nullable = false)
    private int loginCount;

    @Column(nullable = false)
    private int spentEnergy;

    @Column(nullable = false)
    private int killedMonsters;

    private LocalDate lastLoginDay;

    @Column(nullable = false)
    private Instant updatedAt;

    /** 서버가 정한 현재 기간에 빈 행동 집계를 만든다. */
    public UserQuestPeriod(User user, QuestPeriod period, LocalDate day, Instant now) {
        this.user = user;
        this.period = period;
        this.periodDay = day;
        this.updatedAt = now;
    }

    /** 해당 일일 기간의 최초 로그인 처리에서만 호출한다. */
    public void login(LocalDate day, Instant now) {
        loginCount = Math.min(7, loginCount + 1);
        lastLoginDay = day;
        updatedAt = now;
    }

    /** 이미 인정한 전투 누적 값의 양수 차이만 집계한다. */
    public void kill(int amount, Instant now) {
        killedMonsters = (int) Math.min(Integer.MAX_VALUE, (long) killedMonsters + amount);
        updatedAt = now;
    }

    /** 실제 전투 입장이 소비한 양만 누적하고 저장 상한에서 포화시킨다. */
    public void spend(int amount, Instant now) {
        spentEnergy = (int) Math.min(Integer.MAX_VALUE, (long) spentEnergy + amount);
        updatedAt = now;
    }
}
