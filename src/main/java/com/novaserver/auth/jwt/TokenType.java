package com.novaserver.auth.jwt;

/** 토큰 용도. 인증 필터는 ACCESS만, 재발급은 REFRESH만 받는다. */
public enum TokenType {
    ACCESS,
    REFRESH
}
