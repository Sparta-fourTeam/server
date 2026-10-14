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

/** {@code onEvent} 효과 발동 시점. */
@Getter
@Entity
@Table(name = "effect_trigger")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EffectTrigger extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(nullable = false)
    private String label;

    /**
     * 발동 시점을 생성한다.
     *
     * @param name 유니티에 내려가는 이름
     * @param label 관리 화면 표시 이름
     */
    public EffectTrigger(String name, String label) {
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
