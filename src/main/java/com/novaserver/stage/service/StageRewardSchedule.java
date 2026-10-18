package com.novaserver.stage.service;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 수치를 직접 보관하지 않는 지급 일정 계산 모델. 값은 RewardRules 기준 데이터로 받는다. */
public record StageRewardSchedule(
        @NotNull @Schema(description = "시작 지급량. 0도 명시") Integer start,
        @NotNull @Schema(description = "완료 웨이브당 증가량. 0도 명시") Integer perWave,
        @NotNull @Schema(description = "총량과 기본 누적의 차액을 지급할 웨이브, 1~20") Integer bonusWave) {
    public static final int WAVE_COUNT = 20;

    /** 잘못되거나 int 누적량을 초과하는 기준 데이터는 사용하지 않는다. */
    public boolean valid() {
        return start != null
                && perWave != null
                && bonusWave != null
                && start >= 0
                && perWave >= 0
                && bonusWave >= 1
                && bonusWave <= WAVE_COUNT
                && (long) start + (long) perWave * WAVE_COUNT <= Integer.MAX_VALUE;
    }

    /** 전체 웨이브를 완료했을 때의 기본 누적량. */
    public int minimumFinal() {
        return Math.addExact(start, Math.multiplyExact(perWave, WAVE_COUNT));
    }

    /** 완료한 웨이브가 없으면 지급하지 않는다. */
    public int amount(int completedWaves, int finalAmount) {
        if (completedWaves == 0) {
            return 0;
        }
        return Math.addExact(
                Math.addExact(start, Math.multiplyExact(perWave, completedWaves)),
                completedWaves >= bonusWave ? finalAmount - minimumFinal() : 0);
    }
}
