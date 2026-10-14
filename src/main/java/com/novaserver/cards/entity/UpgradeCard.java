package com.novaserver.cards.entity;

import com.novaserver.global.entity.BaseTimeEntity;
import com.novaserver.skill.entity.Skill;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/** 강화 카드. */
@Getter
@Entity
@Table(name = "upgrade_card")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpgradeCard extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String desc;

    @ColumnDefault("1")
    @Column(nullable = false)
    private int maxPickCount;

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean enabled;

    private String disabledReason;

    @ColumnDefault("1")
    @Column(nullable = false)
    private int minBattleLevel;

    @ColumnDefault("0")
    @Column(nullable = false)
    private int minPermanentLevel;

    private String sharedId;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequiredCardCount> requiredCardCounts = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CardExclusion> exclusions = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<CardEffect> effects = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("minPermanentLevel ASC")
    private List<CardVariant> variants = new ArrayList<>();

    /** 강화 카드를 생성한다. */
    public UpgradeCard(
            Skill skill,
            String name,
            String desc,
            Integer maxPickCount,
            Boolean enabled,
            String disabledReason,
            Integer minBattleLevel,
            Integer minPermanentLevel,
            String sharedId) {
        this.skill = skill;
        this.name = name;
        this.desc = desc;
        this.maxPickCount = maxPickCount != null ? maxPickCount : 1;
        this.enabled = enabled == null || enabled;
        this.disabledReason = disabledReason;
        this.minBattleLevel = minBattleLevel != null ? minBattleLevel : 1;
        this.minPermanentLevel = minPermanentLevel != null ? minPermanentLevel : 0;
        this.sharedId = sharedId;
    }

    /** 카드 기본 정보를 수정한다. */
    public void update(
            Skill skill,
            String name,
            String desc,
            int maxPickCount,
            boolean enabled,
            String disabledReason,
            int minBattleLevel,
            int minPermanentLevel,
            String sharedId) {
        this.skill = skill;
        this.name = name;
        this.desc = desc;
        this.maxPickCount = maxPickCount;
        this.enabled = enabled;
        this.disabledReason = disabledReason;
        this.minBattleLevel = minBattleLevel;
        this.minPermanentLevel = minPermanentLevel;
        this.sharedId = sharedId;
    }
}
