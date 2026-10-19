package com.novaserver.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.novaserver.auth.dto.GuestSignUpResponse;
import com.novaserver.auth.dto.TokenResponse;
import com.novaserver.auth.repository.AccessTokenRepository;
import com.novaserver.auth.repository.RefreshTokenRepository;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.auth.service.AuthService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 다른 기기에서 같은 GAME 계정으로 새로 로그인하면 기존 SSE 연결이 forceLogout 이벤트를 받고 끊기는지 확인한다. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GameSseForceLogoutIntegrationTest {

    @LocalServerPort private int port;
    @Autowired private AuthService authService;

    // 단일 세션 강제 로직 자체(Redis 조회)는 다른 테스트가 커버한다. 여기서는 그 조회 결과와 무관하게
    // 새 로그인 시 SSE가 끊기는지만 보므로, 실제 Redis 상태(다른 테스트가 남긴 키 등)에 흔들리지 않도록 목으로 둔다.
    @MockitoBean private AccessTokenRepository accessTokenRepository;
    @MockitoBean private RefreshTokenRepository refreshTokenRepository;
    @MockitoBean private TokenBlocklistRepository tokenBlocklistRepository;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void shouldForceLogoutPreviousDevice_whenSameAccountLogsInAgain() throws Exception {
        GuestSignUpResponse signUp = authService.guestSignUp();
        String firstDeviceToken = signUp.getTokens().getAccessToken();

        CompletableFuture<HttpResponse<Stream<String>>> subscribeFuture =
                client.sendAsync(subscribeRequest(firstDeviceToken), BodyHandlers.ofLines());

        // SSE 구독이 서버에 실제로 등록된 뒤에(connect 이벤트 수신) 두 번째 로그인을 트리거해야 한다.
        // 그 전에 로그인하면 아직 등록 안 된 연결이라 끊을 대상이 없어 테스트가 타이밍에 따라 흔들린다.
        CompletableFuture<Void> connected = new CompletableFuture<>();
        CompletableFuture<String> forceLogoutLine =
                CompletableFuture.supplyAsync(() -> readEvents(subscribeFuture, connected));
        connected.get(5, TimeUnit.SECONDS);

        // 같은 계정(guestId)으로 다른 기기가 다시 로그인한다
        TokenResponse secondDeviceTokens = authService.guestLogIn(signUp.getGuestId());

        assertThat(secondDeviceTokens.getAccessToken()).isNotBlank();
        assertThat(forceLogoutLine.get(5, TimeUnit.SECONDS)).contains("다른 기기에서 로그인");
    }

    // Stream을 닫아야 서버 쪽 SseEmitter 연결도 같이 끊긴다.
    private String readEvents(
            CompletableFuture<HttpResponse<Stream<String>>> future,
            CompletableFuture<Void> connected) {
        try (Stream<String> body = future.join().body()) {
            Iterator<String> lines = body.iterator();
            String currentEvent = null;
            while (lines.hasNext()) {
                String line = lines.next();
                if (line.startsWith("event:")) {
                    currentEvent = line.substring("event:".length());
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                if ("connect".equals(currentEvent)) {
                    connected.complete(null);
                } else if ("forceLogout".equals(currentEvent)) {
                    return line;
                }
            }
            return "";
        }
    }

    private HttpRequest subscribeRequest(String token) {
        return HttpRequest.newBuilder(URI.create(url("/notifications/subscribe")))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }
}
