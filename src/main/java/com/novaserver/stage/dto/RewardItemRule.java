package com.novaserver.stage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 아이템의 보상 분류. */
public record RewardItemRule(
        @NotNull @Schema(description = "SkillMaterial, EquipmentMaterial, GemChest 중 하나")
                String kind,
        @NotNull @Schema(description = "추첨 전용 ID 여부. true면 실제 Items와 다른 ID여야 함. false도 명시")
                Boolean random) {}
