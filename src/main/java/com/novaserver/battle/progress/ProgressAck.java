package com.novaserver.battle.progress;

import java.util.Set;

/** 서버 최초 접수 시각과 기간을 보존하는 누적 진행 ACK. 전투 보상 영수증과 다르다. */
public record ProgressAck(
        String battleId,
        Integer stageId,
        int policyVersion,
        long revision,
        int killedMonsters,
        int completedWaves,
        int reachedWave,
        Set<Long> encounteredMonsterIds,
        int acceptedDelta,
        String acceptedAt,
        String dailyKey,
        String weeklyKey) {}
