package com.novaserver.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Web refresh 토큰을 담는 HttpOnly 쿠키를 만든다.
 *
 * <p>발급·삭제 쿠키가 같은 이름·경로·속성을 쓰도록 쿠키 생성을 이 클래스에 모은다. 하나라도 다르면 브라우저가 다른 쿠키로 보고 덮어쓰거나 지우지 못한다.
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    /** 쿠키 이름. 컨트롤러가 {@code @CookieValue}로 읽을 때도 쓴다. */
    public static final String NAME = "refresh_token";

    private static final String PATH = "/api/auth/web";

    private final TokenValidityProperties validity;

    @Value("${app.cookie.secure}")
    private boolean secure;

    /**
     * Refresh 토큰을 담은 쿠키를 만든다. 쿠키 수명은 Web refresh 유효기간과 같다.
     *
     * @param refreshToken 담을 refresh 토큰
     * @return Set-Cookie 헤더로 내려줄 쿠키
     */
    public ResponseCookie create(String refreshToken) {
        return base(refreshToken).maxAge(validity.of(ClientType.WEB).refresh()).build();
    }

    /**
     * 브라우저의 refresh 쿠키를 지우는 쿠키를 만든다.
     *
     * @return 수명이 0인 같은 이름·경로의 쿠키
     */
    public ResponseCookie expire() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);
    }
}
