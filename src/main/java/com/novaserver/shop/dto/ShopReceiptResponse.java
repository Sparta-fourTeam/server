package com.novaserver.shop.dto;

import com.novaserver.shop.entity.UserShopReceipt;
import java.time.Instant;
import java.util.Map;

/** 최초 차감·실제 보상·처리 직후 진열 영수증. 현재 잔액과 진열은 user/me 또는 shop으로 읽는다. */
public record ShopReceiptResponse(
        String requestId,
        String operation,
        String dayKey,
        int requestedReroll,
        Integer slot,
        int goldSpent,
        int gemsSpent,
        int gold,
        int energy,
        Map<String, Integer> items,
        ShopStateResponse stateAfter,
        Instant completedAt) {
    /** 최초 저장 영수증을 복사해 재전송 결과로 반환한다. */
    public static ShopReceiptResponse from(UserShopReceipt r) {
        return new ShopReceiptResponse(
                r.getRequestId(),
                r.getOperation(),
                r.getDayKey(),
                r.getRequestedReroll(),
                r.getSlot(),
                r.getGoldSpent(),
                r.getGemsSpent(),
                r.getGold(),
                r.getEnergy(),
                Map.copyOf(r.getItems()),
                r.getStateAfter(),
                r.getCompletedAt());
    }
}
