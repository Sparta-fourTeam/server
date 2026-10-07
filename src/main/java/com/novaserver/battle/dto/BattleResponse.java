package com.novaserver.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 배틀 ID 발급 응답. */
@Getter
@RequiredArgsConstructor
@Schema(description = "배틀 ID 발급 응답")
public class BattleResponse {
    @Schema(description = "발급된 배틀 ID", example = "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c")
    private final String battleId;
}
