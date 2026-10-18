package com.novaserver.shop.entity;

import com.novaserver.reward.entity.RewardDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 상점 후보의 잼 가격·에너지와 기존 공통 골드·아이템 묶음 연결. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopOfferDefinition {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int energy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private RewardDefinition rewardDefinition;

    /** 검증된 상품 후보를 만든다. */
    public ShopOfferDefinition(String id, int price, int energy, RewardDefinition reward) {
        this.id = id;
        this.price = price;
        this.energy = energy;
        this.rewardDefinition = reward;
    }

    /** 검증한 전체 교체에서 공통 묶음 연결과 가격을 갱신한다. */
    public void configure(int price, int energy, RewardDefinition reward) {
        this.price = price;
        this.energy = energy;
        this.rewardDefinition = reward;
    }
}
