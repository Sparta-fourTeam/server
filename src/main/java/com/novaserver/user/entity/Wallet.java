package com.novaserver.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저 재화. */
@Entity
@Table(name = "wallet", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private int gold;

    @Column(nullable = false)
    private int gem;

    /** 주어진 골드·젬으로 지갑을 만든다. */
    public Wallet(User user, int gold, int gem) {
        this.user = user;
        this.gold = gold;
        this.gem = gem;
    }

    /** 골드를 차감한다. 충분한지는 호출하는 쪽에서 미리 확인한다. */
    public void useGold(int amount) {
        this.gold -= amount;
    }

    /** 젬을 차감한다. 유료 구매 서비스에서 잔액과 가격을 먼저 검증한다. */
    public void useGem(int amount) {
        this.gem -= amount;
    }

    /** 골드를 지급한다. */
    public void addGold(int amount) {
        this.gold += amount;
    }
}
