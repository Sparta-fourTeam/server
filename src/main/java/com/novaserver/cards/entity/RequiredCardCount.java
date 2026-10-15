package com.novaserver.cards.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/** 선행 카드의 선택 성공 횟수 조건. */
@Getter
@Entity
@Table(name = "required_card_count")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequiredCardCount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upgrade_card_id")
    private UpgradeCard card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "required_card_id")
    private UpgradeCard requiredCard;

    @ColumnDefault("1")
    @Column(nullable = false)
    private int count;

    /** 선행 카드 조건을 생성한다. */
    public RequiredCardCount(UpgradeCard card, UpgradeCard requiredCard, Integer count) {
        this.card = card;
        this.requiredCard = requiredCard;
        this.count = count != null ? count : 1;
    }
}
