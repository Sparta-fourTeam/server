package com.novaserver.auth.jwt;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * GAME 클라이언트는 계정당 WebSocket 연결도 1개만 허용한다.
 *
 * <p>{@link JwtAuthenticationFilter}의 세션 교체 체크는 새 HTTP 요청(핸드셰이크 포함)이 와야 실행되므로, 이미 맺어진 WebSocket 연결은
 * 그 자체로는 끊기지 않는다. 그래서 새 GAME 로그인 시점에 {@link #disconnectExisting}으로 이전 연결을 직접 끊어준다.
 */
@Slf4j
@Component
public class GameWebSocketSessionRegistry implements WebSocketHandlerDecoratorFactory {

    private static final String CLIENT_GAME_AUTHORITY = "CLIENT_" + ClientType.GAME.name();

    private final Map<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<Long, String> sessionIdByUserId = new ConcurrentHashMap<>();

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessionsById.put(session.getId(), session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus)
                    throws Exception {
                sessionsById.remove(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }

    /** GAME 사용자의 STOMP 연결이 확정되면 userId와 세션 ID를 기록한다. */
    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Long userId = gameUserId(event.getUser());
        if (userId == null) {
            return;
        }
        String sessionId = SimpMessageHeaderAccessor.wrap(event.getMessage()).getSessionId();
        sessionIdByUserId.put(userId, sessionId);
    }

    /** 세션이 끊기면, 그게 현재 기록된 세션일 때만 기록에서 지운다. */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Long userId = gameUserId(event.getUser());
        if (userId != null) {
            sessionIdByUserId.remove(userId, event.getSessionId());
        }
    }

    /** 지정한 userId로 열려있는 GAME WebSocket 연결이 있으면 끊는다. 다른 기기의 새 로그인으로 세션이 교체됐을 때 호출한다. */
    public void disconnectExisting(Long userId) {
        String sessionId = sessionIdByUserId.remove(userId);
        if (sessionId == null) {
            return;
        }
        WebSocketSession session = sessionsById.get(sessionId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException e) {
            log.warn("이전 WebSocket 세션 종료 실패. userId={}", userId, e);
        }
    }

    private Long gameUserId(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            return null;
        }
        boolean isGame =
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(CLIENT_GAME_AUTHORITY));
        return isGame ? (Long) authentication.getPrincipal() : null;
    }
}
