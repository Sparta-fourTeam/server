package com.novaserver.skill.entity;

import com.novaserver.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 스킬 시전 방식. name은 유니티가 그대로 받는 값이다. */
@Entity
@Table(name = "skill_cast_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkillCastType extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(nullable = false)
    private String label;

    /**
     * 시전 방식을 생성한다.
     *
     * @param name 유니티에 내려가는 이름
     * @param label 관리 화면 표시 이름
     */
    public SkillCastType(String name, String label) {
        this.name = name;
        this.label = label;
    }

    /**
     * 이름과 표시 이름을 수정한다.
     *
     * @param name 유니티에 내려가는 이름
     * @param label 관리 화면 표시 이름
     */
    public void update(String name, String label) {
        this.name = name;
        this.label = label;
    }
}
