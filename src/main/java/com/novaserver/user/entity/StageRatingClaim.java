package com.novaserver.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 최초 별 보상의 수령 구간과 실제 지급 내역을 계정별 요청 키로 보존한다. */
@Entity
@Table(
        name = "stage_rating_claim",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "create_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StageRatingClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String createId;

    @Column(nullable = false)
    private Integer stageId;

    @Column(nullable = false)
    private int previousClaimedRating;

    @Column(nullable = false)
    private int clearRating;

    @Column(nullable = false)
    private int claimedRating;

    @Column(nullable = false)
    private int gold;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Integer> items = new HashMap<>();

    @Column(nullable = false)
    private Instant completedAt;

    /** 지급과 같은 트랜잭션에서 최초 수령 영수증을 저장한다. */
    public StageRatingClaim(
            Long userId,
            String createId,
            Integer stageId,
            int previousClaimedRating,
            int clearRating,
            int claimedRating,
            int gold,
            Map<String, Integer> items,
            Instant completedAt) {
        this.userId = userId;
        this.createId = createId;
        this.stageId = stageId;
        this.previousClaimedRating = previousClaimedRating;
        this.clearRating = clearRating;
        this.claimedRating = claimedRating;
        this.gold = gold;
        this.items.putAll(items);
        this.completedAt = completedAt;
    }
}
