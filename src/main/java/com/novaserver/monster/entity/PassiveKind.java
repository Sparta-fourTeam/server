package com.novaserver.monster.entity;

/** 패시브 종류. 클라이언트 레지스트리에서 실행 코드를 찾는 키라 이름이 클라이언트 표기와 같아야 한다. */
public enum PassiveKind {
    Spawn,
    Immunity,
    Modifier,
    Shield,
    Evade,
    LowHp,
    HealOnHit,
    SpeedBoost,
}
