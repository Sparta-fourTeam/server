package com.novaserver.shop.entity;

import com.novaserver.shop.dto.ShopStateResponse;
import com.novaserver.user.entity.User;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 계정별 요청 키의 최초 구매·재추첨 결과. 설정·날짜가 바뀌어도 재지급 없이 복원한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "request_id"}))
public class UserShopReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(nullable = false, length = 16)
    private String operation;

    @Column(nullable = false, length = 8)
    private String dayKey;

    @Column(nullable = false)
    private int requestedReroll;

    private Integer slot;

    @Column(nullable = false)
    private int goldSpent;

    @Column(nullable = false)
    private int gemsSpent;

    @Column(nullable = false)
    private int gold;

    @Column(nullable = false)
    private int energy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Integer> items;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private ShopStateResponse stateAfter;

    @Column(nullable = false)
    private Instant completedAt;

    /** 차감·지급·진열 갱신이 성공한 트랜잭션에서 최초 결과를 저장한다. */
    public UserShopReceipt(
            User user,
            String requestId,
            String operation,
            String dayKey,
            int requestedReroll,
            Integer slot,
            int goldSpent,
            int gemsSpent,
            int gold,
            int energy,
            Map<String, Integer> items,
            ShopStateResponse stateAfter,
            Instant completedAt) {
        this.user = user;
        this.requestId = requestId;
        this.operation = operation;
        this.dayKey = dayKey;
        this.requestedReroll = requestedReroll;
        this.slot = slot;
        this.goldSpent = goldSpent;
        this.gemsSpent = gemsSpent;
        this.gold = gold;
        this.energy = energy;
        this.items = new HashMap<>(items);
        this.stateAfter = stateAfter;
        this.completedAt = completedAt;
    }

    /** 같은 키에 요청 대상을 바꿔 붙이면 재사용을 거절한다. */
    public boolean matches(String key, String operation, String day, int reroll, Integer slot) {
        return requestId.equals(key)
                && this.operation.equals(operation)
                && dayKey.equals(day)
                && requestedReroll == reroll
                && Objects.equals(this.slot, slot);
    }
}
