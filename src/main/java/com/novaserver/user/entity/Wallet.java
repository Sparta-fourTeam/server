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
    private int jem;

    public Wallet(User user, int gold, int jem) {
        this.user = user;
        this.gold = gold;
        this.jem = jem;
    }
}
