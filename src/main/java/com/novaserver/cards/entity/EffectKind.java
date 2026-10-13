package com.novaserver.cards.entity;

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

/** EffectRegistry가 실행하는 효과 종류. */
@Getter
@Entity
@Table(name = "effect_kind")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EffectKind extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(nullable = false)
    private String label;

    /**
     * 종류를 생성한다.
     *
     * @param name EffectRegistry에 등록된 key
     * @param label 관리 화면 표시 이름
     */
    public EffectKind(String name, String label) {
        this.name = name;
        this.label = label;
    }

    /**
     * 이름과 표시 이름을 수정한다.
     *
     * @param name EffectRegistry에 등록된 key
     * @param label 관리 화면 표시 이름
     */
    public void update(String name, String label) {
        this.name = name;
        this.label = label;
    }
}
