package com.novaserver.condex.dto;

import com.novaserver.condex.entity.CodexGradeReward;

/** 등급 설정 버전과 공유 보상의 현재 버전·골드를 함께 제공한다. */
public record CodexGradeRewardResponse(
        String grade, Long rewardDefinitionId, int gold, int version, int rewardVersion) {
    /** 실제 서버 보상 정의의 현재 골드를 조회한다. */
    public static CodexGradeRewardResponse from(CodexGradeReward row) {
        var reward = row.getRewardDefinition();
        return new CodexGradeRewardResponse(
                row.getGrade().getExternalName(),
                reward.getId(),
                reward.getGold(),
                row.getVersion(),
                reward.getVersion());
    }
}
