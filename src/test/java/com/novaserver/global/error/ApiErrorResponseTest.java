package com.novaserver.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/** CustomErrorAttributes + WebConfig(/api 프리픽스) + 보안 설정이 실제로 맞물려 동작하는지 확인한다. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiErrorResponseTest {

    @LocalServerPort private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void returnsFieldErrorMessageOnValidationFailure() throws Exception {
        HttpResponse<String> response = post("/api/battle/start", "{}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"message\":\"유저 ID는 필수입니다\"");
        assertThat(response.body()).doesNotContain("timestamp");
    }

    @Test
    void returnsCommonMessageOnMalformedBody() throws Exception {
        HttpResponse<String> response = post("/api/battle/start", "{not-json");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"message\":\"요청 형식이 올바르지 않습니다\"");
    }

    @Test
    void returnsNotFoundForUnknownPathUnderApiPrefix() throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(url("/api/no-such-path"))).GET().build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(url(path)))
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString(body))
                        .build();
        return client.send(request, BodyHandlers.ofString());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
