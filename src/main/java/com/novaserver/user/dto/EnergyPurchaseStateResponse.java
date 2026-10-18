package com.novaserver.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/** 현재 구매 기간의 조회 결과. 최초 거래 영수증과 달리 매번 현재 상태를 반환한다. */
public record EnergyPurchaseStateResponse(
        @Schema(description = "조회 기준 서버 UTC 시각(ISO8601 Z)") Instant serverTime,
        @Schema(description = "한국시간 04시 기준 현재 구매 영업일") LocalDate purchaseDay,
        @Schema(description = "현재 영업일의 확정 구매 횟수") int purchaseCount,
        @Schema(description = "현재 영업일의 남은 구매 횟수. 0이면 새 구매 불가") int remainingPurchases,
        @Schema(description = "다음 한국시간 04시의 UTC 시각(ISO8601 Z)") Instant resetsAt,
        @Schema(description = "한 번 구매할 때 필요한 gem") int gemCost,
        @Schema(description = "한 번 구매할 때 지급하는 에너지") int energyGranted,
        @Schema(description = "영업일별 최대 구매 횟수") int dailyLimit) {}
