package com.novaserver.shop.dto;

import com.novaserver.shop.entity.UserGoldPackReceipt;
import java.time.Instant;

/** 최초 잼 차감·골드 지급 영수증. 현재 잔액은 기존 계정 조회로 복원한다. */
public record GoldPackReceiptResponse(
        String requestId,
        int configVersion,
        int index,
        int gold,
        int gemsSpent,
        int gemGoldRate,
        int discountPercent,
        Instant completedAt) {
    /** 저장한 최초 기준과 실제 금액을 재전송 결과로 반환한다. */
    public static GoldPackReceiptResponse from(UserGoldPackReceipt r) {
        return new GoldPackReceiptResponse(
                r.getRequestId(),
                r.getConfigVersion(),
                r.getPackIndex(),
                r.getGold(),
                r.getGemsSpent(),
                r.getGemGoldRate(),
                r.getDiscountPercent(),
                r.getCompletedAt());
    }
}
