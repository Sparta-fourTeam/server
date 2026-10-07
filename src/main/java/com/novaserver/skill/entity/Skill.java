package com.novaserver.skill.entity;

import com.novaserver.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 스킬 밸런스 데이터. 유니티 Skills 테이블 한 행에 대응한다.
 */
@Getter
@Entity
@Table(name = "skill")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Skill extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String desc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cast_type_id")
    private SkillCastType castType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projectile_path_id")
    private SkillProjectilePath projectilePath;

    @Column(nullable = false)
    private int maxLevel;

    @Column(nullable = false)
    private boolean childOnly;

    @Embedded
    private SkillBaseStats baseStats;

    @ColumnDefault("1")
    @Column(nullable = false)
    private int unlockLevel;

    /**
     * 스킬을 생성한다.
     *
     * @param name           표시 이름
     * @param desc           설명. 없으면 null
     * @param castType       시전 방식
     * @param projectilePath 투사체 경로
     * @param maxLevel       최대 강화 횟수
     * @param childOnly      자식 전용 여부
     * @param baseStats      기본 전투 수치. 없으면 null
     * @param unlockLevel    해금 레벨. null이면 1
     */
    public Skill(
        String name,
        String desc,
        SkillCastType castType,
        SkillProjectilePath projectilePath,
        int maxLevel,
        boolean childOnly,
        SkillBaseStats baseStats,
        Integer unlockLevel) {
        this.name = name;
        this.desc = desc;
        this.castType = castType;
        this.projectilePath = projectilePath;
        this.maxLevel = maxLevel;
        this.childOnly = childOnly;
        this.baseStats = baseStats;
        this.unlockLevel = unlockLevel != null ? unlockLevel : 1;
    }

    /**
     * 스킬 정보를 수정한다. baseStats는 통째로 교체한다.
     *
     * @param name           표시 이름
     * @param desc           설명. 없으면 null
     * @param castType       시전 방식
     * @param projectilePath 투사체 경로
     * @param maxLevel       최대 강화 횟수
     * @param childOnly      자식 전용 여부
     * @param baseStats      기본 전투 수치. 없으면 null
     * @param unlockLevel    해금 레벨. null이면 1
     */
    public void update(
        String name,
        String desc,
        SkillCastType castType,
        SkillProjectilePath projectilePath,
        int maxLevel,
        boolean childOnly,
        SkillBaseStats baseStats,
        Integer unlockLevel) {
        this.name = name;
        this.desc = desc;
        this.castType = castType;
        this.projectilePath = projectilePath;
        this.maxLevel = maxLevel;
        this.childOnly = childOnly;
        this.baseStats = baseStats;
        this.unlockLevel = unlockLevel != null ? unlockLevel : 1;
    }
}
