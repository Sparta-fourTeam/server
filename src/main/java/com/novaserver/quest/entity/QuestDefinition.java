package com.novaserver.quest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 퀘스트의 주기·서버 행동·목표·수령 점수 정의. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestDefinition {
    @Id
    @Column(length = 64)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestKind kind;

    @Column(nullable = false)
    private int target;

    @Column(nullable = false)
    private int points;

    /** 검증된 퀘스트 조건을 만든다. */
    public QuestDefinition(String id, QuestPeriod period, QuestKind kind, int target, int points) {
        this.id = id;
        this.period = period;
        this.kind = kind;
        this.target = target;
        this.points = points;
    }
}
