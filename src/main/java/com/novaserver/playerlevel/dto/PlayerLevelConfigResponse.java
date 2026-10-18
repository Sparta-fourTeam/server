package com.novaserver.playerlevel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 서버 모드와 서버 구매 검증이 함께 사용하는 플레이어 경험치 기준. */
public record PlayerLevelConfigResponse(
        @Schema(description = "레벨 1→2 필요 경험치") int baseExp,
        @Schema(description = "레벨 P→P+1 필요 경험치 = baseExp + stepExp*(P-1)") int stepExp,
        @Schema(description = "도달 가능한 최고 레벨") int maxLevel,
        @Schema(description = "PlayerLevels 데이터 버전. 관리 API 저장 시 전체 revision과 함께 증가") int version) {}
