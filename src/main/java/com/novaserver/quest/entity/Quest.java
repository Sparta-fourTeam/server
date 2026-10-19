package com.novaserver.quest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 일일·주간 퀘스트 정의. 달성하면 points만큼 해당 주기의 누적 포인트에 더해진다. */
@Entity
@Table(name = "quest")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quest {
    @Id private String questId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestKind kind;

    @Column(nullable = false)
    private int target;

    @Column(nullable = false)
    private int points;

    /** 퀘스트를 생성한다. */
    public Quest(String questId, QuestPeriod period, QuestKind kind, int target, int points) {
        this.questId = questId;
        this.period = period;
        this.kind = kind;
        this.target = target;
        this.points = points;
    }

    /** 퀘스트 내용을 수정한다. questId·period는 바꿀 수 없다. */
    public void update(QuestKind kind, int target, int points) {
        this.kind = kind;
        this.target = target;
        this.points = points;
    }
}
