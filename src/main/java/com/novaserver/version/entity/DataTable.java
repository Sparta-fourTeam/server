package com.novaserver.version.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 클라이언트와 버전을 맞추는 게임 데이터 테이블 목록. */
@Getter
@RequiredArgsConstructor
public enum DataTable {
    MONSTERS("Monsters"),
    STAGES("Stages"),
    UPGRADES("Upgrades"),
    ENERGY("Energy"),
    CARDS("Cards");

    private final String key;
}
