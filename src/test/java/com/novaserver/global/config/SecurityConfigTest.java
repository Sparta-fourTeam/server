package com.novaserver.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.JwtAccessDeniedHandler;
import com.novaserver.auth.jwt.JwtAuthenticationEntryPoint;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.jwt.TokenValidityProperties;
import com.novaserver.auth.jwt.TokenValidityProperties.Validity;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.Role;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import({
    SecurityConfig.class,
    JwtProvider.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class,
    SecurityConfigTest.TestController.class
})
@TestPropertySource(
        properties = {
            "app.jwt.secret=" + SecurityConfigTest.SECRET,
            "app.jwt.validity.game.access=15m",
            "app.jwt.validity.game.refresh=30d",
            "app.jwt.validity.web.access=15m",
            "app.jwt.validity.web.refresh=1h"
        })
class SecurityConfigTest {

    static final String SECRET = "test-secret-key-test-secret-key-0123456789"; // gitleaks:allow

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @MockitoBean private TokenBlocklistRepository tokenBlocklistRepository;

    @Test
    @DisplayName("토큰 없이 보호된 API를 호출하면 401 '로그인이 필요합니다'")
    void shouldReturnUnauthorized_whenNoToken() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/player/me"));

        // then
        result.andExpect(error(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("유효한 access 토큰이면 통과하고 컨트롤러가 userId를 받는다")
    void shouldPassAndProvideUserId_whenAccessTokenIsValid() throws Exception {
        // given
        String token = jwtProvider.createAccessToken(42L, ClientType.GAME, Role.USER);

        // when
        ResultActions result = callWithToken("/api/player/me", token);

        // then
        result.andExpect(status().isOk()).andExpect(content().string("42"));
    }

    @Test
    @DisplayName("만료된 토큰이면 401 '토큰이 만료되었습니다'")
    void shouldReturnTokenExpired_whenTokenIsExpired() throws Exception {
        // given
        String token = expiredAccessToken();

        // when
        ResultActions result = callWithToken("/api/player/me", token);

        // then
        result.andExpect(error(ErrorCode.TOKEN_EXPIRED));
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰이면 401 '유효하지 않은 토큰입니다'")
    void shouldReturnInvalidToken_whenSignedWithOtherKey() throws Exception {
        // given
        JwtProvider otherKeyProvider =
                new JwtProvider("other-secret-key-other-secret-key-0123456789", validity(900));
        String token = otherKeyProvider.createAccessToken(42L, ClientType.GAME, Role.USER);

        // when
        ResultActions result = callWithToken("/api/player/me", token);

        // then
        result.andExpect(error(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("refresh 토큰으로는 API를 호출할 수 없다")
    void shouldReturnInvalidToken_whenRefreshTokenIsUsed() throws Exception {
        // given
        String token = jwtProvider.createRefreshToken(42L, ClientType.GAME, Role.USER).token();

        // when
        ResultActions result = callWithToken("/api/player/me", token);

        // then
        result.andExpect(error(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("허용 경로는 만료된 토큰이 붙어 있어도 통과한다")
    void shouldPass_whenPublicPathHasExpiredToken() throws Exception {
        // given
        String token = expiredAccessToken();

        // when
        ResultActions result = callWithToken("/api/auth/ping", token);

        // then
        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("토큰 없이 DELETE /api/auth/me를 호출하면 401 '로그인이 필요합니다'")
    void shouldReturnUnauthorized_whenDeletingAuthMeWithoutToken() throws Exception {
        // when
        ResultActions result = mockMvc.perform(delete("/api/auth/me"));

        // then
        result.andExpect(error(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("유효한 토큰으로 DELETE /api/auth/me를 호출하면 통과한다")
    void shouldPass_whenDeletingAuthMeWithValidToken() throws Exception {
        // given
        String token = jwtProvider.createAccessToken(42L, ClientType.GAME, Role.USER);

        // when
        ResultActions result =
                mockMvc.perform(
                        delete("/api/auth/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("토큰 없이 POST /api/auth/social/link를 호출하면 401 '로그인이 필요합니다'")
    void shouldReturnUnauthorized_whenLinkingSocialWithoutToken() throws Exception {
        // when
        ResultActions result = mockMvc.perform(post("/api/auth/social/link"));

        // then
        result.andExpect(error(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("유효한 토큰으로 POST /api/auth/social/link를 호출하면 통과한다")
    void shouldPass_whenLinkingSocialWithValidToken() throws Exception {
        // given
        String token = jwtProvider.createAccessToken(42L, ClientType.GAME, Role.USER);

        // when
        ResultActions result =
                mockMvc.perform(
                        post("/api/auth/social/link")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 경로에 USER 토큰이면 403 '권한이 없습니다'")
    void shouldReturnForbidden_whenUserAccessesAdminPath() throws Exception {
        // given
        String token = jwtProvider.createAccessToken(42L, ClientType.WEB, Role.USER);

        // when
        ResultActions result = callWithToken("/api/admin/ping", token);

        // then
        result.andExpect(error(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("관리자 경로에 ADMIN 토큰이면 clientType과 상관없이 통과한다")
    void shouldPass_whenAdminAccessesAdminPath() throws Exception {
        // given
        String token = jwtProvider.createAccessToken(1L, ClientType.GAME, Role.ADMIN);

        // when
        ResultActions result = callWithToken("/api/admin/ping", token);

        // then
        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("도감 관리자 경로는 ADMIN GAME 토큰을 거절한다")
    void shouldRejectGameAdmin_onCodexConfigurationPath() throws Exception {
        String token = jwtProvider.createAccessToken(1L, ClientType.GAME, Role.ADMIN);
        callWithToken("/api/admin/codex/ping", token).andExpect(error(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("도감 관리자 경로는 ADMIN WEB 토큰을 허용한다")
    void shouldAllowWebAdmin_onCodexConfigurationPath() throws Exception {
        String token = jwtProvider.createAccessToken(1L, ClientType.WEB, Role.ADMIN);
        callWithToken("/api/admin/codex/ping", token).andExpect(status().isOk());
    }

    private ResultActions callWithToken(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static String expiredAccessToken() {
        return new JwtProvider(SECRET, validity(-60))
                .createAccessToken(42L, ClientType.GAME, Role.USER);
    }

    private static TokenValidityProperties validity(long seconds) {
        Validity validity = new Validity(Duration.ofSeconds(seconds), Duration.ofSeconds(seconds));
        return new TokenValidityProperties(validity, validity);
    }

    /** MockMvc는 sendError를 /error로 넘기지 않으므로 상태 코드와 에러 메시지를 직접 확인한다. */
    private static ResultMatcher error(ErrorCode errorCode) {
        return result -> {
            status().is(errorCode.getStatus().value()).match(result);
            assertThat(result.getResponse().getErrorMessage()).isEqualTo(errorCode.getMessage());
        };
    }

    @RestController
    static class TestController {

        @GetMapping("/player/me")
        Long me(@AuthenticationPrincipal Long userId) {
            return userId;
        }

        @GetMapping("/auth/ping")
        String authPing() {
            return "ok";
        }

        @DeleteMapping("/auth/me")
        String deleteMe() {
            return "ok";
        }

        @PostMapping("/auth/social/link")
        String linkSocial() {
            return "ok";
        }

        @GetMapping("/admin/codex/ping")
        String codexAdminPing() {
            return "ok";
        }

        @GetMapping("/admin/ping")
        String adminPing() {
            return "ok";
        }
    }
}
