package com.novaserver.user.dto;

import com.novaserver.user.entity.UserUpgrade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 유저 강화 응답. */
@Getter
@RequiredArgsConstructor
@Schema(description = "유저 강화 응답")
public class UserUpgradeResponse {
    @Schema(description = "강화 id")
    private final String upgradeId;

    @Schema(description = "강화 레벨")
    private final Integer level;

    public static UserUpgradeResponse from(UserUpgrade userUpgrade) {
        return new UserUpgradeResponse(
                userUpgrade.getUpgrade().getUpgradeId(), userUpgrade.getLevel());
    }
}
