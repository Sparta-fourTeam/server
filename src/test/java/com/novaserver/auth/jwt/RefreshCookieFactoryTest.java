package com.novaserver.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.novaserver.auth.jwt.TokenValidityProperties.Validity;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshCookieFactoryTest {

    private static final TokenValidityProperties VALIDITY =
            new TokenValidityProperties(
                    new Validity(Duration.ofMinutes(15), Duration.ofDays(30)),
                    new Validity(Duration.ofMinutes(15), Duration.ofHours(1)));

    @Test
    @DisplayName("refresh 쿠키는 HttpOnly·Secure·Strict, 경로 /api/auth/web, 수명은 WEB refresh 유효기간")
    void shouldCreateHttpOnlyCookie_withWebRefreshLifetime() {
        // when
        ResponseCookie cookie = factory(true).create("refresh-token");

        // then
        assertThat(cookie.getName()).isEqualTo(RefreshCookieFactory.NAME);
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/auth/web");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("삭제 쿠키는 발급 쿠키와 이름·경로가 같고 수명이 0이다")
    void shouldExpireCookie_withSameNameAndPath() {
        // given
        RefreshCookieFactory factory = factory(true);
        ResponseCookie issued = factory.create("refresh-token");

        // when
        ResponseCookie expired = factory.expire();

        // then
        assertThat(expired.getName()).isEqualTo(issued.getName());
        assertThat(expired.getPath()).isEqualTo(issued.getPath());
        assertThat(expired.getMaxAge()).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("app.cookie.secure가 false면 Secure 속성을 붙이지 않는다 (로컬 HTTP용)")
    void shouldNotBeSecure_whenSecureDisabled() {
        assertThat(factory(false).create("refresh-token").isSecure()).isFalse();
    }

    private static RefreshCookieFactory factory(boolean secure) {
        RefreshCookieFactory factory = new RefreshCookieFactory(VALIDITY);
        ReflectionTestUtils.setField(factory, "secure", secure);
        return factory;
    }
}
