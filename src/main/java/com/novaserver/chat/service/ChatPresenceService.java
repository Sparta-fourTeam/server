package com.novaserver.chat.service;

import com.novaserver.chat.dto.ChatPresenceResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * /topic/chat/presence 구독자 수를 추적하고, 바뀔 때마다 그 수를 같은 채널에 방송한다.
 *
 * <p>세션당 하나의 구독만 센다. 같은 세션이 presence를 두 번 구독해도 중복으로 세지 않고, UNSUBSCRIBE 프레임은 destination을 담지 않아서
 * 세션별로 구독 id를 직접 기억해 본인 구독이 끊길 때만 집계에서 뺀다.
 */
@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private static final String DESTINATION = "/topic/chat/presence";

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, String> subscriptionIdBySession = new ConcurrentHashMap<>();

    /** Presence 채널 구독을 집계하고 인원수를 다시 방송한다. */
    public void onSubscribe(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        if (!DESTINATION.equals(accessor.getDestination())) {
            return;
        }
        subscriptionIdBySession.put(accessor.getSessionId(), accessor.getSubscriptionId());
        broadcast();
    }

    /** 본인의 presence 구독이 끊기면 집계에서 빼고 인원수를 다시 방송한다. */
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        if (subscriptionIdBySession.remove(accessor.getSessionId(), accessor.getSubscriptionId())) {
            broadcast();
        }
    }

    /** 소켓 연결이 끊기면 그 세션을 집계에서 빼고 인원수를 다시 방송한다. */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        if (subscriptionIdBySession.remove(event.getSessionId()) != null) {
            broadcast();
        }
    }

    private void broadcast() {
        messagingTemplate.convertAndSend(
                DESTINATION, new ChatPresenceResponse(subscriptionIdBySession.size()));
    }
}
