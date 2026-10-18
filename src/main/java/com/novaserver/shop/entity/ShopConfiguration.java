package com.novaserver.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 버전으로 전체 교체하는 일일 상점 정책. 출시 기본값은 자동 입력하지 않는다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopConfiguration {
    @Id private Long id = 1L;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private int slots;

    @Column(nullable = false)
    private int goldRerollsPerDay;

    @Column(nullable = false)
    private int goldRerollCost;

    @Column(nullable = false)
    private int adRerollsPerDay;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Integer> discounts;

    /** 검증한 새 서버 정책을 만든다. */
    public ShopConfiguration(
            int version, int slots, int goldLimit, int cost, int adLimit, List<Integer> discounts) {
        this.version = version;
        this.slots = slots;
        goldRerollsPerDay = goldLimit;
        goldRerollCost = cost;
        adRerollsPerDay = adLimit;
        this.discounts = List.copyOf(discounts);
    }
}
