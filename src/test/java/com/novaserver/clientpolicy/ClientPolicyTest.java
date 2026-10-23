package com.novaserver.clientpolicy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ClientPolicyTest {
    private ClientPolicy policy(boolean enabled, String mode, boolean legacy) {
        return new ClientPolicy(
                1,
                "test",
                enabled,
                mode,
                "점검",
                null,
                30,
                legacy,
                Map.of(
                        "android",
                        new ClientPolicy.PlatformPolicy(
                                "10", "12", "https://play.google.com/store/apps/details?id=test")));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @CsvSource({"1,1.0,0", "1.10,1.9,1", "9,10,-1", "001,1,0"})
    void comparesNumericBuildComponents(String left, String right, int expected) {
        assertThat(Integer.signum(BuildNumber.compare(left, right))).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-1", "1-beta", "1.2.3.4.5", "10000000000", "1\n"})
    void rejectsMalformedBuilds(String value) {
        assertThat(BuildNumber.valid(value)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "android,9,CLIENT_UPDATE_REQUIRED",
        "android,10,ALLOW",
        "android,11,ALLOW",
        "android,bad,CLIENT_UPDATE_REQUIRED",
        "unknown,11,CLIENT_UPDATE_REQUIRED",
        ",11,CLIENT_UPDATE_REQUIRED",
        "android,,CLIENT_UPDATE_REQUIRED",
        ",,CLIENT_UPDATE_REQUIRED"
    })
    void validatesPlatformAndMinimum(String platform, String build, String expected) {
        String result =
                ClientPolicyFilter.rejection(
                        policy(true, "NORMAL", false), "/api/player/me", platform, build);
        assertThat(result == null ? "ALLOW" : result).isEqualTo(expected);
    }

    @Test
    void offAndLegacyRemainCompatible() {
        // Given / When / Then: 최초 배포와 명시적 legacy 허용은 기존 앱을 잠그지 않는다.
        assertThat(
                        ClientPolicyFilter.rejection(
                                policy(false, "MAINTENANCE", false),
                                "/api/battle/start",
                                null,
                                null))
                .isNull();
        assertThat(
                        ClientPolicyFilter.rejection(
                                policy(true, "NORMAL", true), "/api/player/me", null, null))
                .isNull();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/battle/start",
                "/api/auth/guest/signup",
                "/api/auth/guest/login",
                "/api/auth/social/login"
            })
    void drainingRejectsNewEntry(String path) {
        assertThat(
                        ClientPolicyFilter.rejection(
                                policy(true, "DRAINING", true), path, "android", "12"))
                .isEqualTo("DRAINING");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/battle/end",
                "/api/battle/cancel",
                "/api/battle/progress",
                "/api/battle/progress/open",
                "/api/battle/progress/abandon",
                "/api/battle/progress/existing-id",
                "/api/auth/refresh"
            })
    void completionAndRefreshSurviveDrainingAndMinimumVersion(String path) {
        assertThat(ClientPolicyFilter.rejection(policy(true, "DRAINING", false), path, null, null))
                .isNull();
        assertThat(
                        ClientPolicyFilter.rejection(
                                policy(true, "MAINTENANCE", true), path, "android", "12"))
                .isEqualTo("MAINTENANCE");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/battle/end",
                "/api/battle/progress",
                "/api/auth/refresh",
                "/api/auth/logout",
                "/api/energy/charge",
                "/api/shop/exchange",
                "/api/auth/me"
            })
    void maintenanceStopsGameWriterBeforeHandler(String path) throws Exception {
        authenticate("CLIENT_GAME");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ClientPolicyFilter(() -> policy(true, "MAINTENANCE", true))
                .doFilter(new MockHttpServletRequest("POST", path), response, chain);
        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("MAINTENANCE", "policyRevision");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/notices", "/api/auth/web/login", "/api/client/status"})
    void webAdminAndStatusAreNotGameGated(String path) throws Exception {
        authenticate("CLIENT_WEB");
        MockFilterChain chain = new MockFilterChain();
        new ClientPolicyFilter(() -> policy(true, "MAINTENANCE", false))
                .doFilter(
                        new MockHttpServletRequest("POST", path),
                        new MockHttpServletResponse(),
                        chain);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void unauthenticatedStatusIsNotGameGated() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        new ClientPolicyFilter(ClientPolicy::unavailable)
                .doFilter(
                        new MockHttpServletRequest("GET", "/api/client/status"),
                        new MockHttpServletResponse(),
                        chain);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void encodedOrParameterizedNewEntryCannotBypassDraining() throws Exception {
        authenticate("CLIENT_GAME");
        for (String path : List.of("/api/battle/%73tart", "/api/battle/start;param=1")) {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = new MockHttpServletResponse();
            new ClientPolicyFilter(() -> policy(true, "DRAINING", true))
                    .doFilter(new MockHttpServletRequest("POST", path), response, chain);
            assertThat(chain.getRequest()).isNull();
            assertThat(response.getStatus()).isEqualTo(503);
        }
    }

    @Test
    void webTokenCannotBypassMaintenanceOnGameplayWriter() throws Exception {
        authenticate("CLIENT_WEB");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ClientPolicyFilter(ClientPolicy::unavailable)
                .doFilter(new MockHttpServletRequest("POST", "/api/battle/start"), response, chain);
        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(503);
    }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                1L, null, List.of(new SimpleGrantedAuthority(authority))));
    }
}
