package com.novaserver.auth.jwt;

/**
 * 검증을 통과한 토큰에서 꺼낸 값.
 *
 * @param playerId 토큰 주인 (sub)
 * @param clientType 발급 대상 클라이언트 (aud)
 * @param tokenType 토큰 용도 (type)
 */
public record TokenInfo(Long playerId, ClientType clientType, TokenType tokenType) {}
