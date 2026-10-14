package com.novaserver.user.dto;

import com.novaserver.user.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "유저 상태 정보 응답")
public class UserDataResponse {
    @Schema(description = "골드")
    private final int gold;

    @Schema(description = "젬")
    private final int jem;

    @Schema(description = "경험치")
    private final Integer exp;

    @Schema(description = "저장된 에너지")
    private final int energyStored;

    @Schema(description = "에너지 갱신 시각")
    private final LocalDateTime energyUpdatedAt;

    @Schema(description = "스테이지 진행도 목록")
    private final List<UserStageProgressResponse> stageProgress;

    @Schema(description = "강화 목록")
    private final List<UserUpgradeResponse> upgrades;

    @Schema(description = "아이템 목록")
    private final List<UserItemResponse> items;

    @Schema(description = "도감 목록")
    private final List<UserCodexResponse> codex;

    @Schema(description = "완료한 튜토리얼 id 목록")
    private final List<Long> tutorials;

    public static UserDataResponse of(
            UserData userData,
            Wallet wallet,
            UserEnergy energy,
            List<UserStageProgress> stageProgress,
            List<UserUpgrade> upgrades,
            List<UserItem> items,
            List<UserCodex> codex,
            List<UserTutorial> tutorials) {
        return new UserDataResponse(
                wallet.getGold(),
                wallet.getJem(),
                userData.getExp(),
                energy.getEnergyStored(),
                energy.getEnergyUpdatedAt(),
                stageProgress.stream().map(UserStageProgressResponse::from).toList(),
                upgrades.stream().map(UserUpgradeResponse::from).toList(),
                items.stream().map(UserItemResponse::from).toList(),
                codex.stream().map(UserCodexResponse::from).toList(),
                tutorials.stream().map(ut -> ut.getTutorials().getId()).toList());
    }
}
