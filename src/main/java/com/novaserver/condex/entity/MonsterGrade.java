package com.novaserver.condex.entity;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 클라이언트 EnemyType의 정확한 등급 이름. 몬스터 능력치·처치 증거와는 별개다. */
@Getter
@RequiredArgsConstructor
public enum MonsterGrade {
    NORMAL("Normal"),
    ELITE("Elite"),
    BOSS("Boss");
    private final String externalName;

    /** 대소문자 별칭 없이 실제 데이터의 등급 이름만 허용한다. */
    public static Optional<MonsterGrade> fromName(String value) {
        return Arrays.stream(values()).filter(g -> g.externalName.equals(value)).findFirst();
    }
}
