package com.novaserver.user.entity;

import com.novaserver.user.EnergyRecoverySource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 계정별 최초 회복 결과를 UTC 시각과 함께 영구 보존하는 멱등 영수증. */
@Entity
@Table(
        name = "user_energy_recovery_receipt",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "create_id"}),
        indexes =
                @Index(
                        name = "idx_energy_purchase_day",
                        columnList = "user_id,source,purchase_day"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEnergyRecoveryReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 128)
    private String createId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnergyRecoverySource source;

    @Column(nullable = false)
    private int energyGranted;

    @Column(nullable = false)
    private int gemsSpent;

    @Column(nullable = false)
    private LocalDate purchaseDay;

    @Column(nullable = false)
    private int purchaseCount;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    private Instant completedAt;

    /** 차감·지급과 같은 트랜잭션에서 확정한 최초 구매 결과를 기록한다. */
    public UserEnergyRecoveryReceipt(
            User user,
            String createId,
            EnergyRecoverySource source,
            int energyGranted,
            int gemsSpent,
            LocalDate purchaseDay,
            int purchaseCount,
            Instant completedAt) {
        this.user = user;
        this.createId = createId;
        this.source = source;
        this.energyGranted = energyGranted;
        this.gemsSpent = gemsSpent;
        this.purchaseDay = purchaseDay;
        this.purchaseCount = purchaseCount;
        this.completedAt = completedAt;
    }
}
