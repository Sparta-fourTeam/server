package com.novaserver.skill.entity;

import com.novaserver.cards.entity.UpgradeCard;
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

/** 다른 스킬에서 고른 카드의 효과를 이 스킬의 시전에 함께 적용하는 조건. */
@Getter
@Entity
@Table(name = "mirrored_card")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MirroredCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private UpgradeCard card;

    /** 카드 효과 적용 조건을 생성한다. */
    public MirroredCard(Skill skill, UpgradeCard card) {
        this.skill = skill;
        this.card = card;
    }
}
