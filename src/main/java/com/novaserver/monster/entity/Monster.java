package com.novaserver.monster.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 몬스터 밸런스 데이터. 클라이언트 Monsters 테이블 한 행에 대응한다. id는 기획 ID를 그대로 쓴다. */
@Getter
@Entity
@Table(name = "monster")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Monster {

    @Id private Long id;

    @Column(nullable = false)
    private int hp;

    @Column(nullable = false)
    private int damage;

    @Column(nullable = false)
    private double speed;

    @Column(nullable = false)
    private double attackInterval;

    @Column(nullable = false)
    private double attackRange;

    @Column(nullable = false)
    private double projectileSpeed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnemyType grade;

    @Column(nullable = false)
    private int burstCount;

    @Column(nullable = false)
    private double burstInterval;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<Element, Double> resists = new EnumMap<>(Element.class);

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<CastType, Double> castResists = new EnumMap<>(CastType.class);

    @Column(nullable = false)
    private boolean blocksProjectile;

    @OneToMany(mappedBy = "monster", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<MonsterPassive> passives = new ArrayList<>();

    /** 몬스터를 생성한다. 생략 가능한 값은 시트의 생략 기본값으로 채운다. */
    public Monster(
            Long id,
            Integer hp,
            Integer damage,
            Double speed,
            Double attackInterval,
            Double attackRange,
            Double projectileSpeed,
            EnemyType grade,
            Integer burstCount,
            Double burstInterval,
            Map<Element, Double> resists,
            Map<CastType, Double> castResists,
            Boolean blocksProjectile) {
        this.id = id;
        this.hp = hp;
        this.damage = damage;
        this.speed = speed;
        this.attackInterval = attackInterval;
        this.attackRange = attackRange;
        this.projectileSpeed = (projectileSpeed != null) ? projectileSpeed : 0;
        this.grade = grade;
        this.burstCount = (burstCount != null) ? burstCount : 1;
        this.burstInterval = (burstInterval != null) ? burstInterval : 0;
        this.resists = new EnumMap<>(Element.class);
        this.castResists = new EnumMap<>(CastType.class);
        this.blocksProjectile = blocksProjectile != null && blocksProjectile;

        if (resists != null) {
            this.resists.putAll(resists);
        }

        if (castResists != null) {
            this.castResists.putAll(castResists);
        }
    }

    public void addPassive(MonsterPassive passive) {
        passives.add(passive);
        passive.assignMonster(this);
    }

    public boolean hasSpawnPassive() {
        return this.passives.stream().anyMatch(p -> p.getKind() == PassiveKind.Spawn);
    }
}
