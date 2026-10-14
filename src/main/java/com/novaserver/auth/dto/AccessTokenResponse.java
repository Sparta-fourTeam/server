package com.novaserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Web 로그인·재발급 응답. refresh 토큰은 HttpOnly 쿠키로 내려가므로 access 토큰만 담는다.
 *
 * @param accessToken access 토큰
 */
@Schema(description = "Web 로그인·재발급 응답 (refresh 토큰은 쿠키로 전달)")
public record AccessTokenResponse(@Schema(description = "access 토큰") String accessToken) {}
