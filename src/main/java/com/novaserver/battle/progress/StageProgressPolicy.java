package com.novaserver.battle.progress;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 동일 버전 원본에서 공급할 스폰 범위·출시 배속. 이어하기 시간 만료는 두지 않는다. */
public record StageProgressPolicy(
        BigDecimal maxTimeScale,
        BigDecimal minSpawnInterval,
        List<Map<Long, Integer>> waves,
        Map<Long, List<SpawnLimit>> descendants,
        int expectedVersion) {
    /** 사망 분열은 Count, 주기 소환은 해당 개체의 MaxTotal을 서버 데이터에서 가져온다. */
    public record SpawnLimit(Long monsterId, int maximum) {}
}
