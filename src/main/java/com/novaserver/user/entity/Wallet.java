package com.novaserver.user.entity;

import jakarta.persistence.*;
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
}
