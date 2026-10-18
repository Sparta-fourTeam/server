package com.novaserver.tutorial.dto;

import com.novaserver.tutorial.entity.Tutorials;

/** 클라이언트 표시 및 관리자 확인용 튜토리얼 조건. 보상 수량은 공통 정의에서 관리한다. */
public record TutorialConfigResponse(
        String id,
        String prerequisiteId,
        int minLevel,
        Integer startStageId,
        Integer clearStageId,
        String upgradeId,
        int upgradeLevel,
        int playerLevel,
        int setLevel,
        Long rewardDefinitionId) {
    /** 저장된 조건을 ID 기반 응답으로 변환한다. */
    public static TutorialConfigResponse from(Tutorials t) {
        return new TutorialConfigResponse(
                t.getId(),
                t.getPrerequisite() == null ? null : t.getPrerequisite().getId(),
                t.getMinLevel(),
                t.getStartStage() == null ? null : t.getStartStage().getStageId(),
                t.getClearStage() == null ? null : t.getClearStage().getStageId(),
                t.getRequiredUpgrade() == null ? null : t.getRequiredUpgrade().getUpgradeId(),
                t.getUpgradeLevel(),
                t.getPlayerLevel(),
                t.getSetLevel(),
                t.getRewardDefinition().getId());
    }
}
