package com.novaserver.user.dto;

import com.novaserver.user.entity.StageRatingClaim;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/** 현재 잔액과 구분하는 최초 별 보상 수령 영수증. */
public record StageRatingClaimResponse(
        String createId,
        Integer stageId,
        @Schema(description = "이번 수령 직전 수령 등급. 이 값보다 높은 구간만 지급") int previousClaimedRating,
        int clearRating,
        int claimedRating,
        @Schema(description = "이번 수령에서 지급한 골드. 현재 잔액이 아님") int gold,
        @Schema(description = "이번 수령에서 지급한 실제 아이템 ID와 수량") Map<String, Integer> items,
        @Schema(description = "최초 수령 완료 UTC 시각") Instant completedAt) {
    /** 현재 진행도가 아닌 저장된 최초 수령 결과를 반환한다. */
    public static StageRatingClaimResponse from(StageRatingClaim claim) {
        return new StageRatingClaimResponse(
                claim.getCreateId(),
                claim.getStageId(),
                claim.getPreviousClaimedRating(),
                claim.getClearRating(),
                claim.getClaimedRating(),
                claim.getGold(),
                Map.copyOf(claim.getItems()),
                claim.getCompletedAt());
    }
}
