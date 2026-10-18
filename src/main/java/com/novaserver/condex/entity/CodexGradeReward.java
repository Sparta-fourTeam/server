package com.novaserver.condex.entity;

import com.novaserver.reward.entity.RewardDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 몬스터별 중복 설정 없이 등급 하나가 참조하는 공통 골드 보상. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodexGradeReward {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private MonsterGrade grade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private RewardDefinition rewardDefinition;

    @Column(nullable = false)
    private int version = 1;

    /** 관리자가 선택한 기존 공통 보상을 등급에 연결한다. */
    public CodexGradeReward(MonsterGrade grade, RewardDefinition reward) {
        this.grade = grade;
        this.rewardDefinition = reward;
    }

    /** 연결을 교체한다. 과거 계정 영수증은 수정하지 않는다. */
    public void changeReward(RewardDefinition reward) {
        this.rewardDefinition = reward;
        version = Math.addExact(version, 1);
    }
}
