package com.novaserver.auth.jwt;

/**
 * 새로 발급한 토큰과 그 토큰의 고유 ID.
 *
 * @param token 클라이언트에 내려줄 토큰 문자열
 * @param tokenId 토큰 고유 ID (jti). refresh 저장소에 저장해 재발급 때 비교한다
 */
public record IssuedToken(String token, String tokenId) {}
