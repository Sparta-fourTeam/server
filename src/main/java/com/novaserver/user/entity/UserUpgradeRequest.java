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

/** 유저 강화 레벨업 요청 기록. 같은 createId로 다시 요청하면 이 결과를 그대로 돌려줘 중복 처리를 막는다. */
@Entity
@Table(
        name = "user_upgrade_request",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "upgrade_id", "create_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserUpgradeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upgrade_id")
    private Upgrade upgrade;

    @Column(nullable = false)
    private String createId;

    @Column(nullable = false)
    private Integer resultLevel;

    private Integer goldSpent;

    private Integer materialSpent;

    /** 레벨업 요청 결과를 기록한다. */
    public UserUpgradeRequest(
            User user,
            Upgrade upgrade,
            String createId,
            Integer resultLevel,
            Integer goldSpent,
            Integer materialSpent) {
        this.user = user;
        this.upgrade = upgrade;
        this.createId = createId;
        this.resultLevel = resultLevel;
        this.goldSpent = goldSpent;
        this.materialSpent = materialSpent;
    }
}
