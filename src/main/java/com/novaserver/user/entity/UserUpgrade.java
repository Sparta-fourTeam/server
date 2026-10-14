package com.novaserver.user.entity;

import com.novaserver.upgrade.entity.Upgrade;
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

/** 유저별 강화 정도. */
@Entity
@Table(
        name = "user_upgrade",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "upgrade_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserUpgrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upgrade_id")
    private Upgrade upgrade;

    @Column(name = "level", nullable = false)
    private Integer level;
}
