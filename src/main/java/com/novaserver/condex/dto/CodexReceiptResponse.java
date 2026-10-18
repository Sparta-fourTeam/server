package com.novaserver.condex.dto;

import com.novaserver.user.entity.UserCodex;
import java.time.Instant;

/** 최초 확정한 골드 영수증. 현재 잔액은 user/me로 조회한다. */
public record CodexReceiptResponse(Long monsterId, int gold, Instant claimedAt) {
    /** 현재 보상 기준을 다시 읽지 않고 보존된 최초 지급값을 반환한다. */
    public static CodexReceiptResponse from(UserCodex row) {
        return new CodexReceiptResponse(
                row.getCodex().getId(), row.getClaimedGold(), row.getClaimedAt());
    }
}
