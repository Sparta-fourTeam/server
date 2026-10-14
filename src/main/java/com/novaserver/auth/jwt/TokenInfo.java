package com.novaserver.auth.jwt;

import com.novaserver.user.Role;

/**
 * 검증을 통과한 토큰에서 꺼낸 값.
 *
 * @param userId 토큰 주인 (sub)
 * @param role 토큰 주인의 권한 (role)
 * @param clientType 발급 대상 클라이언트 (aud)
 * @param tokenType 토큰 용도 (type)
 * @param tokenId 토큰 고유 ID (jti)
 */
public record TokenInfo(
        Long userId, Role role, ClientType clientType, TokenType tokenType, String tokenId) {}
