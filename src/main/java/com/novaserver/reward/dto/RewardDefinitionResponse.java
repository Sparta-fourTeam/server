package com.novaserver.reward.dto;

import com.novaserver.reward.entity.RewardDefinition;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/** 공통 고정 보상과 버전·별등급 및 튜토리얼 참조 수. 현재 계정 지급 내역이 아니다. */
public record RewardDefinitionResponse(
        Long id,
        String name,
        int version,
        int gold,
        Map<String, Integer> items,
        @Schema(description = "이 묶음 편집의 영향을 받는 레이팅 보상 수") long ratingReferences,
        @Schema(description = "이 묶음 편집의 영향을 받는 튜토리얼 수") long tutorialReferences) {
    /** 엔티티를 고정 보상 응답으로 변환한다. */
    public static RewardDefinitionResponse from(
            RewardDefinition definition, long references, long tutorialReferences) {
        return new RewardDefinitionResponse(
                definition.getId(),
                definition.getName(),
                definition.getVersion(),
                definition.getGold(),
                definition.itemAmounts(),
                references,
                tutorialReferences);
    }
}
