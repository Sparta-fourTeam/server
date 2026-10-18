package com.novaserver.condex.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 도감용 서버 몬스터 ID·등급. ID는 클라이언트 데이터와 같은 명시적 식별자다. */
@Entity
@Table(name = "codex")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Codex {
    @Id private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonsterGrade grade;

    @Column(nullable = false)
    private int version = 1;

    /** 관리자가 명시한 기존 게임 데이터 ID·등급으로 도감 카탈로그를 만든다. */
    public Codex(Long id, MonsterGrade grade) {
        this.id = id;
        this.grade = grade;
    }

    /** 현재 등급을 교체한다. 이미 지급한 계정 영수증에는 영향을 주지 않는다. */
    public void changeGrade(MonsterGrade grade) {
        this.grade = grade;
        version = Math.addExact(version, 1);
    }
}
