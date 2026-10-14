package com.novaserver.auth.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 클라이언트 종류별 토큰 유효기간. 토큰 만료 시각, Redis TTL, 쿠키 수명이 모두 이 값을 쓴다.
 *
 * @param game 게임 클라이언트 유효기간
 * @param web Web(관리자 페이지) 유효기간
 */
@ConfigurationProperties("app.jwt.validity")
public record TokenValidityProperties(Validity game, Validity web) {

    /**
     * Access·refresh 유효기간.
     *
     * @param access access 토큰 유효기간
     * @param refresh refresh 토큰 유효기간
     */
    public record Validity(Duration access, Duration refresh) {}

    /**
     * 클라이언트 종류에 맞는 유효기간을 반환한다.
     *
     * @param clientType 발급 대상 클라이언트
     * @return 해당 클라이언트의 유효기간
     */
    public Validity of(ClientType clientType) {
        return switch (clientType) {
            case GAME -> game;
            case WEB -> web;
        };
    }
}
