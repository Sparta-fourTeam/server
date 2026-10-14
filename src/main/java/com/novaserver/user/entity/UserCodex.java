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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codex_id")
    private Codex codex;

    @Column(nullable = false)
    private boolean claimed;
}
