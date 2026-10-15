package com.novaserver.user.entity;

import com.novaserver.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 유저 에너지. */
@Entity
@Table(name = "energy", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEnergy {
    public static final int MAX_ENERGY = 30;
    public static final long RECOVERY_INTERVAL_MINUTES = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private int energyStored;

    @Column(nullable = false)
    private LocalDateTime energyUpdatedAt;

    /** 유저와 초기 에너지로 에너지 정보를 만든다. */
    public UserEnergy(User user, int energyStored) {
        this.user = user;
        this.energyStored = energyStored;
        this.energyUpdatedAt = LocalDateTime.now();
    }

    /** 에너지 소모. */
    public void consume(int amount, LocalDateTime now) {
        if (amount <= 0) {
            throw ErrorCode.INVALID_REQUEST.exception();
        }

        if (energyStored < amount) {
            throw ErrorCode.INSUFFICIENT_ENERGY.exception();
        }

        if (energyStored >= MAX_ENERGY) {
            this.energyUpdatedAt = now;
        }

        this.energyStored -= amount;
    }

    /** 자동 회복. */
    public void recover(LocalDateTime now) {
        if (energyStored >= MAX_ENERGY) {
            return;
        }

        long elapsedMinutes = Duration.between(energyUpdatedAt, now).toMinutes();

        long recovered = elapsedMinutes / RECOVERY_INTERVAL_MINUTES;

        if (recovered <= 0) {
            return;
        }

        energyStored = (int) Math.min(MAX_ENERGY, energyStored + recovered);

        if (energyStored < MAX_ENERGY) {
            energyUpdatedAt = energyUpdatedAt.plusMinutes(recovered * RECOVERY_INTERVAL_MINUTES);
        } else {
            energyUpdatedAt = now;
        }
    }

    /** 에너지 보상 획득. */
    public void addEnergy(int amount, LocalDateTime now) {
        if (amount <= 0) {
            throw ErrorCode.INVALID_REQUEST.exception();
        }

        this.energyStored += amount;

        if (this.energyStored >= MAX_ENERGY) {
            this.energyUpdatedAt = now;
        }
    }
}
