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

/** 카드 선택에 필요한 보유 스킬 조건. */
@Getter
@Entity
@Table(name = "required_skill")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequiredSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upgrade_card_id")
    private UpgradeCard card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    /** 필요 스킬 조건을 생성한다. */
    public RequiredSkill(UpgradeCard card, Skill skill) {
        this.card = card;
        this.skill = skill;
    }
}
