package com.novaserver.upgrade.dto;

import com.novaserver.upgrade.entity.UpgradeEffect;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.Getter;

/** 강화 수정 요청. */
@Getter
public class UpgradeRequest {
    @NotNull(message = "최대 레벨은 필수값입니다.")
    @Positive(message = "최대 레벨은 1 이상입니다.")
    private Integer maxLevel;

    @Positive(message = "해금 레벨은 1 이상입니다.")
    @Schema(
            description =
                    "강화를 구매할 수 있는 플레이어 레벨. 강화 maxLevel과 별개. 생략 시 1. 스킬 강화는 연결 스킬의 unlockLevel 사용")
    private Integer unlockLevel;

    private String materialItemId;

    @PositiveOrZero(message = "costBase는 0 이상입니다.")
    private Integer costBase;

    @PositiveOrZero(message = "costStep은 0 이상입니다.")
    private Integer costStep;

    @PositiveOrZero(message = "materialBase는 0 이상입니다.")
    private Integer materialBase;

    @PositiveOrZero(message = "materialStep은 0 이상입니다.")
    private Integer materialStep;

    @Schema(
            description =
                    "현재 강화 레벨 L의 다음 구매 골드 = levelCosts[L]. maxLevel개 양수. "
                            + "null일 때만 costBase + costStep * L")
    private List<@NotNull @Positive Integer> levelCosts;

    @Schema(
            description =
                    "현재 강화 레벨 L의 다음 재료 수량 = materialCosts[L]. maxLevel개 양수. "
                            + "null일 때만 materialBase + materialStep * L")
    private List<@NotNull @Positive Integer> materialCosts;

    private List<UpgradeEffect> effects;
}
