package com.novaserver.monster.entity;

/** 시전 형태. CastResists의 키로 쓰며, 이름은 클라이언트 표기와 같아야 한다. */
public enum CastType {
    Projectile,
    Instant,
    Area,
    Beam,
    Chain,
}
