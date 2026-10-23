package com.novaserver.monster.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

/** 몬스터 패시브. Kind마다 쓰는 값이 달라서, 그 Kind가 쓰지 않는 값은 null로 둔다. */
@Getter
@Entity
@Table(name = "monster_passive")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonsterPassive {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monster_id")
    private Monster monster;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassiveKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "passive_trigger")
    private PassiveTrigger trigger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spawn_monster_id")
    private Monster spawnMonster;

    private Integer count;

    @Column(name = "passive_interval")
    private Double interval;

    private Integer maxTotal;

    @ElementCollection
    @BatchSize(size = 100)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "monster_passive_status",
            joinColumns = @JoinColumn(name = "passive_id"))
    @Column(name = "status")
    private Set<StatusImmunity> statuses = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Target target;

    private Double multiplier;

    private Integer hits;

    private Double duration;

    private Double chance;

    private Double hpRatio;

    private Double healRatio;

    private Boolean invulnerable;

    private Double speedMultiplier;

    @Enumerated(EnumType.STRING)
    private Element element;

    /** 패시브를 생성한다. 그 Kind가 쓰지 않는 값은 넘기지 않아 null로 남긴다. */
    @Builder
    public MonsterPassive(
            PassiveKind kind,
            PassiveTrigger trigger,
            Monster spawnMonster,
            Integer count,
            Double interval,
            Integer maxTotal,
            Set<StatusImmunity> statuses,
            Target target,
            Double multiplier,
            Integer hits,
            Double duration,
            Double chance,
            Double hpRatio,
            Double healRatio,
            Boolean invulnerable,
            Double speedMultiplier,
            Element element) {
        this.kind = kind;
        this.trigger = trigger;
        this.spawnMonster = spawnMonster;
        this.count = count;
        this.interval = interval;
        this.maxTotal = maxTotal;
        this.statuses = (statuses != null) ? new HashSet<>(statuses) : new HashSet<>();
        this.target = target;
        this.multiplier = multiplier;
        this.hits = hits;
        this.duration = duration;
        this.chance = chance;
        this.hpRatio = hpRatio;
        this.healRatio = healRatio;
        this.invulnerable = invulnerable;
        this.speedMultiplier = speedMultiplier;
        this.element = element;
    }

    void assignMonster(Monster monster) {
        this.monster = monster;
    }
}
