package com.novaserver.user.dto;

import com.novaserver.user.EnergyRecoverySource;
import com.novaserver.user.entity.UserEnergyRecoveryReceipt;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/** 최초 확정 회복 영수증. 현재 잔액은 별도 user/me 조회로 확인한다. */
public record EnergyRecoveryResponse(
        String createId,
        EnergyRecoverySource source,
        int energyGranted,
        int gemsSpent,
        @Schema(description = "한국시간 04시를 경계로 한 최초 구매 영업일") LocalDate purchaseDay,
        @Schema(description = "해당 영업일에서 최초 구매 완료 당시의 누적 구매 횟수") int purchaseCount,
        @Schema(description = "최초 확정 UTC 시각(ISO8601 Z). 현재 잔액이나 새 요청 결과가 아님") Instant completedAt) {

    /** 영구 저장한 최초 결과를 반환한다. 같은 키는 이후 구매·날짜 변경에도 동일하다. */
    public static EnergyRecoveryResponse from(UserEnergyRecoveryReceipt receipt) {
        return new EnergyRecoveryResponse(
                receipt.getCreateId(),
                receipt.getSource(),
                receipt.getEnergyGranted(),
                receipt.getGemsSpent(),
                receipt.getPurchaseDay(),
                receipt.getPurchaseCount(),
                receipt.getCompletedAt());
    }
}
