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
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 카드 선택 시 적용하는 개별 효과. */
@Getter
@Entity
@Table(name = "card_effect")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardEffect {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upgrade_card_id")
    private UpgradeCard card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_variant_id")
    private CardVariant variant;

    @Column(nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kind_id")
    private EffectKind kind;

    @Column(nullable = false)
    private double value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id")
    private SkillForm form;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_id")
    private EffectTrigger trigger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @ColumnDefault("1")
    @Column(nullable = false)
    private double chance;

    @ColumnDefault("1")
    @Column(nullable = false)
    private double damageScale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_skill_id")
    private Skill target;

    @ColumnDefault("1")
    @Column(nullable = false)
    private int count;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "only_form_id")
    private SkillForm onlyForm;

    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean excludeHit;

    @Column(name = "interval_seconds")
    private Double interval;

    /** 대상 스킬에 승계할 수치 비율. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inherit_stats")
    private Map<String, Double> inherit;

    /** 효과를 생성한다. */
    public CardEffect(
            UpgradeCard card,
            CardVariant variant,
            int sortOrder,
            EffectKind kind,
            Double value,
            SkillForm form,
            EffectTrigger trigger,
            Skill skill,
            Double chance,
            Double damageScale,
            Skill target,
            Integer count,
            SkillForm onlyForm,
            Boolean excludeHit,
            Double interval,
            Map<String, Double> inherit) {
        this.card = card;
        this.variant = variant;
        this.sortOrder = sortOrder;
        this.kind = kind;
        this.value = value != null ? value : 0;
        this.form = form;
        this.trigger = trigger;
        this.skill = skill;
        this.chance = chance != null ? chance : 1;
        this.damageScale = damageScale != null ? damageScale : 1;
        this.target = target;
        this.count = count != null ? count : 1;
        this.onlyForm = onlyForm;
        this.excludeHit = excludeHit != null && excludeHit;
        this.interval = interval;
        this.inherit = inherit;
    }

    /** 효과 내용을 수정한다. */
    public void update(
            int sortOrder,
            EffectKind kind,
            Double value,
            SkillForm form,
            EffectTrigger trigger,
            Skill skill,
            Double chance,
            Double damageScale,
            Skill target,
            Integer count,
            SkillForm onlyForm,
            Boolean excludeHit,
            Double interval,
            Map<String, Double> inherit) {
        this.sortOrder = sortOrder;
        this.kind = kind;
        this.value = value != null ? value : 0;
        this.form = form;
        this.trigger = trigger;
        this.skill = skill;
        this.chance = chance != null ? chance : 1;
        this.damageScale = damageScale != null ? damageScale : 1;
        this.target = target;
        this.count = count != null ? count : 1;
        this.onlyForm = onlyForm;
        this.excludeHit = excludeHit != null && excludeHit;
        this.interval = interval;
        this.inherit = inherit;
    }
}
