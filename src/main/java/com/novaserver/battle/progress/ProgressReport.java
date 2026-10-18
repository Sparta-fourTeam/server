package com.novaserver.battle.progress;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Set;

/** 보고 ID와 이전 ACK revision을 고정한 누적 진행. 클라이언트 시각이나 보상은 받지 않는다. */
public record ProgressReport(
        @NotBlank String battleId,
        @NotBlank String reportId,
        @Min(0) long expectedRevision,
        @Min(0) int killedMonsters,
        @Min(0) int completedWaves,
        @Min(0) int reachedWave,
        @NotNull Set<@NotNull @Positive Long> encounteredMonsterIds) {}
