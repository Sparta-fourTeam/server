package com.novaserver.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP 기반 WebSocket 메시징 설정. 전체 채팅(/topic/chat)과 접속자 수(/topic/chat/presence) 채널을 쓴다.
 *
 * <p>핸드셰이크는 일반 HTTP 요청으로 처리되어 {@code SecurityConfig}의 JWT 인증을 그대로 거친다. 인증된 사용자만 {@code
 * /api/ws-chat}에 연결할 수 있고, 연결된 세션의 Principal은 JWT에서 꺼낸 사용자다. 경로를 {@code /api}로 시작한 건 배포 환경 nginx가 이
 * 접두사가 있는 요청만 스프링 서버로 넘기기 때문이다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String ENDPOINT = "/api/ws-chat";
    private static final String APPLICATION_PREFIX = "/app";
    private static final String BROKER_PREFIX = "/topic";

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(ENDPOINT).setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes(APPLICATION_PREFIX);
        registry.enableSimpleBroker(BROKER_PREFIX);
    }
}
