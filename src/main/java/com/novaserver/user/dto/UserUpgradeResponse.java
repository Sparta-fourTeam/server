package com.novaserver.user.dto;

import com.novaserver.user.entity.UserUpgrade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "유저 강화 응답")
public class UserUpgradeResponse {
    @Schema(description = "강화 id")
    private final Long upgradeId;

    @Schema(description = "강화 레벨")
    private final Integer level;

    public static UserUpgradeResponse from(UserUpgrade userUpgrade) {
        return new UserUpgradeResponse(userUpgrade.getUpgrade().getId(), userUpgrade.getLevel());
    }
}
