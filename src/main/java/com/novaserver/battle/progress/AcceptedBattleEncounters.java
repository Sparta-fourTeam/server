package com.novaserver.battle.progress;

import java.time.Instant;
import java.util.Set;

/** 서버 스폰 범위 검사를 통과한 신규 조우. 실제 전투 재현으로 조우를 증명한 이벤트는 아니다. */
public record AcceptedBattleEncounters(
        Long userId, String battleId, String reportId, Set<Long> monsterIds, Instant acceptedAt) {}
