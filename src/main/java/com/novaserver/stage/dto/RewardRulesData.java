package com.novaserver.stage.dto;

import com.novaserver.stage.service.StageRewardSchedule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 지급 수치와 아이템 분류를 함께 교체하는 공통 기준 데이터. */
public record RewardRulesData(
        @NotNull @Schema(description = "Coin, SkillMaterial, EquipmentMaterial, GemChest 일정 전체")
                Map<String, StageRewardSchedule> schedules,
        @NotNull @Schema(description = "itemId별 보상 종류 및 랜덤 여부") Map<String, RewardItemRule> items) {
    private static final Set<String> KINDS =
            Set.of("Coin", "SkillMaterial", "EquipmentMaterial", "GemChest");

    /** 누락/지원하지 않는 분류와 범위 초과 일정을 검증한다. */
    public boolean valid() {
        return schedules != null
                && schedules.keySet().equals(KINDS)
                && schedules.values().stream().allMatch(value -> value != null && value.valid())
                && items != null
                && items.entrySet().stream()
                        .allMatch(
                                entry -> {
                                    RewardItemRule rule = entry.getValue();
                                    return entry.getKey() != null
                                            && !entry.getKey().isBlank()
                                            && !entry.getKey().startsWith("currency.")
                                            && rule != null
                                            && rule.kind() != null
                                            && KINDS.contains(rule.kind())
                                            && !rule.kind().equals("Coin")
                                            && rule.random() != null
                                            && !(rule.random() && rule.kind().equals("GemChest"));
                                });
    }

    /** 고정 지급할 수 있는 아이템의 일정. 랜덤 확정 지급은 후속 구현 전까지 차단한다. */
    public Optional<StageRewardSchedule> fixedItemSchedule(String itemId) {
        RewardItemRule rule = items.get(itemId);
        return rule == null || rule.random()
                ? Optional.empty()
                : Optional.of(schedules.get(rule.kind()));
    }
}
