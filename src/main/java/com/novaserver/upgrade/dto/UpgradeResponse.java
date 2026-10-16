package com.novaserver.upgrade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.upgrade.entity.UpgradeEffect;
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
        List<UpgradeEffect> effects) {

    /** 강화 엔티티로 응답을 만든다. */
    public static UpgradeResponse from(Upgrade upgrade) {
        return new UpgradeResponse(
                upgrade.getUpgradeId(),
                upgrade.getMaxLevel(),
                upgrade.getUnlockLevel(),
                upgrade.getMaterialItemId(),
                upgrade.getCostBase(),
                upgrade.getCostStep(),
                upgrade.getMaterialBase(),
                upgrade.getMaterialStep(),
                upgrade.getEffects());
    }
}
