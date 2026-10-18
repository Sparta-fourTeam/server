package com.novaserver.condex.dto;

import com.novaserver.user.entity.UserCodex;
import java.time.Instant;

/** 계정별 최초 발견 시각과 수령 상태·영수증 복원. */
public record CodexStateResponse(
        Long monsterId, boolean claimed, Instant discoveredAt, CodexReceiptResponse receipt) {
    /** 저장 상태만으로 도감과 최초 영수증을 복원한다. */
    public static CodexStateResponse from(UserCodex row) {
        return new CodexStateResponse(
                row.getCodex().getId(),
                row.isClaimed(),
                row.getDiscoveredAt(),
                row.isClaimed() ? CodexReceiptResponse.from(row) : null);
    }
}
