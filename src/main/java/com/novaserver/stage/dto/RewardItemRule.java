package com.novaserver.stage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 아이템의 보상 분류. */
public record RewardItemRule(
        @NotNull @Schema(description = "SkillMaterial, EquipmentMaterial, GemChest 중 하나")
                String kind,
        @NotNull @Schema(description = "전투 보상에서 재료를 추첨하는 정의인지 여부. false도 명시") Boolean random) {}
