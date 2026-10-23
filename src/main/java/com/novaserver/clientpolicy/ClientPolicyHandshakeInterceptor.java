package com.novaserver.clientpolicy;

import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/** HTTP 정책 검사를 통과한 build 정보를 보존한다. STOMP native header로 덮어쓸 수 없다. */
public class ClientPolicyHandshakeInterceptor implements HandshakeInterceptor {
    public static final String IDENTITY_ATTRIBUTE =
            ClientPolicyHandshakeInterceptor.class.getName() + ".identity";

    /** 연결 시점의 공개 앱 식별 정보. 세션 수명 동안 동일한 값으로 검사한다. */
    public record Identity(String platform, String build) {}

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            Map<String, Object> attributes) {
        attributes.put(
                IDENTITY_ATTRIBUTE,
                new Identity(
                        request.getHeaders().getFirst("X-Game-Platform"),
                        request.getHeaders().getFirst("X-Game-Build")));
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            Exception exception) {}
}
