package com.novaserver.notice;

import static org.assertj.core.api.Assertions.assertThat;

import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.repository.AccessTokenRepository;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.user.Role;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
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

/** GAME 클라이언트가 SSE로 접속해 있으면 WEB 관리자가 올린 공지를 즉시 받는지, 실제 HTTP 왕복으로 확인한다. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class NoticeSseIntegrationTest {

    @LocalServerPort private int port;
    @Autowired private JwtProvider jwtProvider;

    // JWT 필터는 실제로 실행하되, 세션·차단 계정 조회 때문에 외부 Redis가 필요하지 않게 한다.
    @MockitoBean private TokenBlocklistRepository tokenBlocklistRepository;
    @MockitoBean private AccessTokenRepository accessTokenRepository;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void shouldReceiveNoticeOverSse_whenAdminPostsNotice() throws Exception {
        String gameToken = jwtProvider.createAccessToken(777L, ClientType.GAME, Role.USER);
        String adminToken = jwtProvider.createAccessToken(778L, ClientType.WEB, Role.ADMIN);

        CompletableFuture<HttpResponse<Stream<String>>> subscribeFuture =
                client.sendAsync(subscribeRequest(gameToken), BodyHandlers.ofLines());

        // SSE 구독이 서버에 실제로 등록된 뒤에(connect 이벤트 수신) 공지를 올려야 한다. 그 전에 올리면
        // broadcast가 아직 등록 안 된 연결을 못 봐서 테스트가 타이밍에 따라 흔들린다.
        CompletableFuture<Void> connected = new CompletableFuture<>();
        CompletableFuture<String> noticeDataLine =
                CompletableFuture.supplyAsync(() -> readEvents(subscribeFuture, connected));
        connected.get(5, TimeUnit.SECONDS);

        HttpResponse<String> createResponse =
                client.send(createNoticeRequest(adminToken), BodyHandlers.ofString());

        assertThat(createResponse.statusCode()).isEqualTo(200);
        assertThat(noticeDataLine.get(5, TimeUnit.SECONDS)).contains("테스트 공지");
    }

    // Stream을 닫아야 서버 쪽 SseEmitter 연결도 같이 끊긴다. 안 닫으면 테스트 종료 후에도
    // 비동기 요청이 살아있어 컨텍스트 셧다운이 타임아웃까지 걸린다.
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
                } else if ("notice".equals(currentEvent)) {
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

    private HttpRequest createNoticeRequest(String token) {
        return HttpRequest.newBuilder(URI.create(url("/admin/notices")))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString("{\"title\":\"점검 안내\",\"content\":\"테스트 공지\"}"))
                .build();
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }
}
