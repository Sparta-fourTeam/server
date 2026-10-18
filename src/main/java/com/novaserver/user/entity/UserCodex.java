package com.novaserver.user.entity;

import com.novaserver.condex.entity.Codex;
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

/** 유저별 도감. */
@Entity
@Table(
        name = "user_codex",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "codex_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCodex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codex_id", nullable = false)
    private Codex codex;

    @Column(nullable = false)
    private boolean claimed;

    @Column(nullable = false)
    private Instant discoveredAt;

    @Column(nullable = false)
    private int claimedGold;

    private Instant claimedAt;

    /** 서버에서 검증한 최초 조우를 저장한다. */
    public UserCodex(User user, Codex codex, Instant discoveredAt) {
        this.user = user;
        this.codex = codex;
        this.discoveredAt = discoveredAt;
    }

    /** 실제 지급한 골드와 최초 수령 시각을 같은 상태 행에 보존한다. */
    public void claim(int gold, Instant now) {
        this.claimed = true;
        this.claimedGold = gold;
        this.claimedAt = now;
    }
}
