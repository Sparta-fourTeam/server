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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 계정별 에너지 구매 기준 날짜·횟수. 해당 지갑의 잠금 아래에서만 변경한다. */
@Entity
@Table(
        name = "user_energy_purchase_state",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEnergyPurchaseState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate purchaseDay;

    @Column(nullable = false)
    private int purchaseCount;

    /** 신규 계정 또는 기존 영수증으로 복원한 일일 구매 상태를 만든다. */
    public UserEnergyPurchaseState(User user, LocalDate purchaseDay, int purchaseCount) {
        this.user = user;
        this.purchaseDay = purchaseDay;
        this.purchaseCount = purchaseCount;
    }

    /** 새 구매 기간이면 날짜와 횟수를 새 기간의 0회로 바꾼다. */
    public void beginPeriod(LocalDate day) {
        if (!purchaseDay.equals(day)) {
            purchaseDay = day;
            purchaseCount = 0;
        }
    }

    /** 결제·지급 성공 시 같은 트랜잭션에서 횟수를 증가시킨다. */
    public void recordPurchase() {
        purchaseCount++;
    }
}
