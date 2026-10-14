package com.novaserver.skill.entity;

import com.novaserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 스킬·몬스터 속성. name은 유니티가 그대로 받는 값이며 몬스터 속성 저항과 연결된다. */
@Entity
@Table(name = "skill_element")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkillElement extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(nullable = false)
    private String label;

    public SkillElement(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public void update(String name, String label) {
        this.name = name;
        this.label = label;
    }
}
