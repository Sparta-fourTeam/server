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
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 계정·기간·정의 ID별 최초 수령 점수와 실제 지급 영수증. 현재 설정과 독립이다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints =
                @UniqueConstraint(
                        columnNames = {"user_id", "period", "period_day", "definition_id"}))
public class UserQuestClaim {
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

    @Column(name = "definition_id", nullable = false, length = 64)
    private String definitionId;

    @Column(nullable = false)
    private boolean chest;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false)
    private int gold;

    @Column(nullable = false)
    private int energy;

    @Column(nullable = false)
    private int gems;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Integer> items;

    @Column(nullable = false)
    private Instant claimedAt;

    /** 지급 성공과 같은 트랜잭션에서만 최초 영수증을 저장한다. */
    public UserQuestClaim(
            User user,
            QuestPeriod period,
            LocalDate day,
            String definitionId,
            boolean chest,
            int points,
            int gold,
            int energy,
            int gems,
            Map<String, Integer> items,
            Instant now) {
        this.user = user;
        this.period = period;
        this.periodDay = day;
        this.definitionId = definitionId;
        this.chest = chest;
        this.points = points;
        this.gold = gold;
        this.energy = energy;
        this.gems = gems;
        this.items = new HashMap<>(items);
        this.claimedAt = now;
    }
}
