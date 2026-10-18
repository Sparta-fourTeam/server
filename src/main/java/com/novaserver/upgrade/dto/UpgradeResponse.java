package com.novaserver.upgrade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.upgrade.entity.UpgradeEffect;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 강화 데이터 응답. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpgradeResponse(
        String upgradeId,
        int maxLevel,
        int unlockLevel,
        String materialItemId,
        Integer costBase,
        Integer costStep,
        Integer materialBase,
        Integer materialStep,
        List<UpgradeEffect> effects,
        List<Integer> levelCosts,
        List<Integer> materialCosts,
        @Schema(description = "게임 구매 해금 플레이어 레벨. 연결된 스킬 중 최소 해금 레벨") Integer effectiveUnlockLevel) {

    /** 강화 엔티티로 응답을 만든다. */
    public static UpgradeResponse from(Upgrade upgrade) {
        return from(upgrade, null);
    }

    /** 게임 조회에 실제 구매 해금 레벨을 함께 전달한다. */
    public static UpgradeResponse from(Upgrade upgrade, Integer effectiveUnlockLevel) {
        return new UpgradeResponse(
                upgrade.getUpgradeId(),
                upgrade.getMaxLevel(),
                upgrade.getUnlockLevel(),
                upgrade.getMaterialItem() == null ? null : upgrade.getMaterialItem().getItemId(),
                upgrade.getCostBase(),
                upgrade.getCostStep(),
                upgrade.getMaterialBase(),
                upgrade.getMaterialStep(),
                upgrade.getEffects(),
                upgrade.getLevelCosts(),
                upgrade.getMaterialCosts(),
                effectiveUnlockLevel);
    }
}
