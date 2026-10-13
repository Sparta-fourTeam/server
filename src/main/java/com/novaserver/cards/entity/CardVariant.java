package com.novaserver.cards.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 영구 레벨 조건에 따라 바뀌는 카드의 변형. */
@Getter
@Entity
@Table(name = "card_variant")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upgrade_card_id")
    private UpgradeCard card;

    @Column(nullable = false)
    private int minPermanentLevel;

    @Column(nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String desc;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<CardEffect> effects = new ArrayList<>();

    /** 변형을 생성한다. */
    public CardVariant(UpgradeCard card, int minPermanentLevel, String name, String desc) {
        this.card = card;
        this.minPermanentLevel = minPermanentLevel;
        this.name = name;
        this.desc = desc;
    }

    /** 변형 내용을 수정한다. */
    public void update(int minPermanentLevel, String name, String desc) {
        this.minPermanentLevel = minPermanentLevel;
        this.name = name;
        this.desc = desc;
    }
}
