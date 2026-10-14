package com.novaserver.cards.entity;

import com.novaserver.skill.entity.Skill;
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

/** 선택한 카드에 따른 단방향 배제 조건. */
@Getter
@Entity
@Table(name = "card_exclusion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardExclusion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upgrade_card_id")
    private UpgradeCard card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "excluding_card_id")
    private UpgradeCard excludingCard;

    @ColumnDefault("0")
    @Column(nullable = false)
    private int belowPermanentLevel;

    /** 배제 조건을 생성한다. */
    public CardExclusion(
            UpgradeCard card, Skill skill, UpgradeCard excludingCard, Integer belowPermanentLevel) {
        this.card = card;
        this.skill = skill;
        this.excludingCard = excludingCard;
        this.belowPermanentLevel = belowPermanentLevel != null ? belowPermanentLevel : 0;
    }
}
