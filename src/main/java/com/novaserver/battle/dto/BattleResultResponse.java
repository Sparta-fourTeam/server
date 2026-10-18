package com.novaserver.battle.dto;

import com.novaserver.battle.entity.BattleResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/** 최초 확정 결과. 현재 잔액이 아니라 해당 전투에서 지급한 보상이다. */
public record BattleResultResponse(
        String battleId,
        Integer stageId,
        int clearedWave,
        int rating,
        @Schema(description = "지급 경험치 총합. threeStarBonusExp가 이미 포함됨") int exp,
        @Schema(description = "총 경험치에 포함된 3성 보너스. 표시 분리용이며 별도로 더하지 않음") int threeStarBonusExp,
        int gold,
        Map<String, Integer> items,
        int clearRating,
        @Schema(description = "최초 완료 서버 UTC 시각(ISO8601 Z)") Instant completedAt) {
    /** 저장된 최초 영수증을 응답으로 변환한다. */
    public static BattleResultResponse from(BattleResult result) {
        return new BattleResultResponse(
                result.getBattleId(),
                result.getStageId(),
                result.getClearedWave(),
                result.getRating(),
                result.getExp(),
                result.getThreeStarBonusExp(),
                result.getGold(),
                Map.copyOf(result.getItems()),
                result.getClearRating(),
                result.getCompletedAt());
    }
}
