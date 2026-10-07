package com.novaserver.auth.jwt;

/** 토큰을 발급받는 클라이언트 종류. GAME은 게임 클라이언트, WEB은 관리자 페이지. */
public enum ClientType {
    GAME,
    WEB
}
