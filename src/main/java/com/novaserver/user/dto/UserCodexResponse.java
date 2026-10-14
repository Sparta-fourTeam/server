package com.novaserver.user.dto;

import com.novaserver.user.entity.UserCodex;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 유저 도감 응답. */
@Getter
@RequiredArgsConstructor
@Schema(description = "유저 도감 응답")
public class UserCodexResponse {
    @Schema(description = "도감에 등록된 몬스터 id")
    private final Long monsterId;

    @Schema(description = "몬스터 등록 골드 보상 수령 여부")
    private final boolean claimed;

    public static UserCodexResponse from(UserCodex userCodex) {
        return new UserCodexResponse(userCodex.getCodex().getId(), userCodex.isClaimed());
    }
}
