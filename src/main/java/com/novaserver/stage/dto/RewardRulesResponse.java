package com.novaserver.stage.dto;

import com.novaserver.stage.service.StageRewardSchedule;
import java.util.Map;

/** 같은 버전의 일정과 아이템 분류를 원자적으로 읽은 응답. */
public record RewardRulesResponse(
        Map<String, StageRewardSchedule> schedules,
        Map<String, RewardItemRule> items,
        int version) {}
