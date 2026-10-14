package com.novaserver.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 유저 에너지. */
@Entity
@Table(name = "energy", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEnergy {
    public static final int MAX_ENERGY = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private int energyStored;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime energyUpdatedAt;

    public UserEnergy(User user, int energyStored) {
        this.user = user;
        this.energyStored = energyStored;
    }
}
