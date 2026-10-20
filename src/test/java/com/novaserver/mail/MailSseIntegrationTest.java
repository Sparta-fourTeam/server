package com.novaserver.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.repository.AccessTokenRepository;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.user.Provider;
import com.novaserver.user.Role;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
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

/**
 * GAME 클라이언트가 SSE로 접속해 있으면, WEB 관리자가 보낸 우편을 제목·내용·경험치·골드·젬·에너지·첨부 아이템 목록까지 즉시 받는지, 그리고 관리자가 유저 우편함을
 * 조회할 수 있는지 실제 HTTP 왕복으로 확인한다.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MailSseIntegrationTest {

    @LocalServerPort private int port;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private ItemRepository itemRepository;

    // JWT 필터는 실제로 실행하되, 세션·차단 계정 조회 때문에 외부 Redis가 필요하지 않게 한다.
    @MockitoBean private TokenBlocklistRepository tokenBlocklistRepository;
    @MockitoBean private AccessTokenRepository accessTokenRepository;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void shouldReceiveMailWithItemsOverSse_whenAdminSendsMail() throws Exception {
        User recipient = userRepository.save(new User(Provider.GUEST, "mail-sse-recipient"));
        if (!itemRepository.existsById("ITEM_POTION")) {
            itemRepository.save(new Item("ITEM_POTION", null));
        }

        String gameToken =
                jwtProvider.createAccessToken(recipient.getId(), ClientType.GAME, Role.USER);
        String adminToken = jwtProvider.createAccessToken(778L, ClientType.WEB, Role.ADMIN);

        CompletableFuture<HttpResponse<Stream<String>>> subscribeFuture =
                client.sendAsync(subscribeRequest(gameToken), BodyHandlers.ofLines());

        // SSE 구독이 서버에 실제로 등록된 뒤에(connect 이벤트 수신) 우편을 보내야 한다. 그 전에 보내면
        // sendTo가 아직 등록 안 된 연결을 못 봐서 테스트가 타이밍에 따라 흔들린다.
        CompletableFuture<Void> connected = new CompletableFuture<>();
        CompletableFuture<String> mailDataLine =
                CompletableFuture.supplyAsync(() -> readEvents(subscribeFuture, connected));
        connected.get(5, TimeUnit.SECONDS);

        HttpResponse<String> sendResponse =
                client.send(
                        sendMailRequest(adminToken, recipient.getId()), BodyHandlers.ofString());

        assertThat(sendResponse.statusCode()).isEqualTo(200);
        String received = mailDataLine.get(5, TimeUnit.SECONDS);
        assertThat(received).contains("출석 보상");
        assertThat(received).contains("꾸준한 접속에 감사드립니다");
        assertThat(received).contains("\"exp\":20");
        assertThat(received).contains("\"gold\":100");
        assertThat(received).contains("\"gem\":10");
        assertThat(received).contains("\"energy\":3");
        assertThat(received).contains("ITEM_POTION");
        assertThat(received).contains("\"quantity\":5");
    }

    @Test
    void shouldReturnUserMailbox_whenAdminGetsMails() throws Exception {
        User recipient = userRepository.save(new User(Provider.GUEST, "mail-admin-lookup"));
        if (!itemRepository.existsById("ITEM_POTION")) {
            itemRepository.save(new Item("ITEM_POTION", null));
        }
        String adminToken = jwtProvider.createAccessToken(779L, ClientType.WEB, Role.ADMIN);

        HttpResponse<String> sendResponse =
                client.send(
                        sendMailRequest(adminToken, recipient.getId()), BodyHandlers.ofString());
        assertThat(sendResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> listResponse =
                client.send(
                        getMailsRequest(adminToken, recipient.getId()), BodyHandlers.ofString());

        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.body()).contains("출석 보상");
        assertThat(listResponse.body()).contains("\"gold\":100");
    }

    @Test
    void shouldReturnSentCountAndStoreMail_whenAdminBulkSends() throws Exception {
        User recipient = userRepository.save(new User(Provider.GUEST, "mail-bulk-recipient"));
        String adminToken = jwtProvider.createAccessToken(780L, ClientType.WEB, Role.ADMIN);

        HttpResponse<String> bulkResponse =
                client.send(
                        bulkMailRequest(adminToken, recipient.getId()), BodyHandlers.ofString());

        assertThat(bulkResponse.statusCode()).isEqualTo(200);
        assertThat(bulkResponse.body()).contains("\"sentCount\":1");

        HttpResponse<String> listResponse =
                client.send(
                        getMailsRequest(adminToken, recipient.getId()), BodyHandlers.ofString());
        assertThat(listResponse.body()).contains("이벤트 보상");
        assertThat(listResponse.body()).contains("\"gold\":50");
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
                } else if ("mail".equals(currentEvent)) {
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

    private HttpRequest sendMailRequest(String token, Long userId) {
        String body =
                """
                {"userId":%d,"title":"출석 보상","content":"꾸준한 접속에 감사드립니다","exp":20,"gold":100,"gem":10,"energy":3,"items":[{"itemId":"ITEM_POTION","quantity":5}]}
                """
                        .formatted(userId);
        return HttpRequest.newBuilder(URI.create(url("/admin/mails")))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest getMailsRequest(String token, Long userId) {
        return HttpRequest.newBuilder(URI.create(url("/admin/mails?userId=" + userId)))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
    }

    private HttpRequest bulkMailRequest(String token, Long userId) {
        String body =
                """
                {"userIds":[%d],"title":"이벤트 보상","content":"참여 감사합니다","gold":50}
                """
                        .formatted(userId);
        return HttpRequest.newBuilder(URI.create(url("/admin/mails/bulk")))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }
}
