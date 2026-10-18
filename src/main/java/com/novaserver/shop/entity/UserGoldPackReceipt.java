package com.novaserver.shop.entity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 계정별 골드팩 요청 키의 최초 차감·지급·가격 기준 영수증. 날짜별로 초기화하지 않는다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "request_id"}))
public class UserGoldPackReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(nullable = false)
    private int configVersion;

    @Column(name = "pack_index", nullable = false)
    private int packIndex;

    @Column(nullable = false)
    private int gold;

    @Column(nullable = false)
    private int gemsSpent;

    @Column(nullable = false)
    private int gemGoldRate;

    @Column(nullable = false)
    private int discountPercent;

    @Column(nullable = false)
    private Instant completedAt;

    /** 차감과 지급이 성공한 동일 트랜잭션에서만 최초 결과를 저장한다. */
    public UserGoldPackReceipt(
            User user,
            String requestId,
            int version,
            int index,
            int gold,
            int price,
            int rate,
            int discount,
            Instant now) {
        this.user = user;
        this.requestId = requestId;
        configVersion = version;
        packIndex = index;
        this.gold = gold;
        gemsSpent = price;
        gemGoldRate = rate;
        discountPercent = discount;
        completedAt = now;
    }

    /** 같은 키에 다른 버전·팩 번호를 붙이면 재사용을 거절한다. */
    public boolean matches(String key, int version, int index) {
        return requestId.equals(key) && configVersion == version && packIndex == index;
    }
}
