package com.novaserver.chat.service;

import com.novaserver.chat.dto.ChatPresenceResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
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
    private static final String DISCONNECTED =
            ChatPresenceService.class.getName() + ".disconnected";
    private static final String SUBSCRIPTIONS =
            ChatPresenceService.class.getName() + ".subscriptions";
    private static final String SUBSCRIPTION_STATE =
            ChatPresenceService.class.getName() + ".subscriptionState";

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, SubscriptionState> subscriptionBySession = new ConcurrentHashMap<>();

    /** 비동기 브로커 처리 전에 구독의 수명을 기록하고, 해제된 구독의 지연 콜백을 무효화한다. */
    public synchronized <T> Message<T> prepareMessage(Message<T> message) {
        var accessor = SimpMessageHeaderAccessor.wrap(message);
        var type = accessor.getMessageType();
        boolean subscribe =
                type == SimpMessageType.SUBSCRIBE && DESTINATION.equals(accessor.getDestination());
        if (!subscribe && type != SimpMessageType.UNSUBSCRIBE) {
            return message;
        }
        var attributes = accessor.getSessionAttributes();
        if (attributes == null || Boolean.TRUE.equals(attributes.get(DISCONNECTED))) {
            return message;
        }
        var subscriptions =
                (SessionSubscriptions)
                        attributes.computeIfAbsent(
                                SUBSCRIPTIONS, ignored -> new SessionSubscriptions());
        SubscriptionState state;
        if (subscribe) {
            state = new SubscriptionState();
            var previous = subscriptions.byId.put(accessor.getSubscriptionId(), state);
            if (previous != null) {
                previous.active = false;
            }
        } else {
            state = subscriptions.byId.remove(accessor.getSubscriptionId());
            if (state == null) {
                return message;
            }
            state.active = false;
        }
        accessor.setHeader(SUBSCRIPTION_STATE, state);
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    /** Presence 채널 구독을 집계하고 인원수를 다시 방송한다. */
    public synchronized void onSubscribe(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        if (!DESTINATION.equals(accessor.getDestination())) {
            return;
        }
        var attributes = accessor.getSessionAttributes();
        if (attributes == null || Boolean.TRUE.equals(attributes.get(DISCONNECTED))) {
            return;
        }
        if (!(accessor.getHeader(SUBSCRIPTION_STATE) instanceof SubscriptionState state)
                || !state.active) {
            return;
        }
        subscriptionBySession.put(accessor.getSessionId(), state);
        broadcast();
    }

    /** 본인의 presence 구독이 끊기면 집계에서 빼고 인원수를 다시 방송한다. */
    public synchronized void onUnsubscribe(SessionUnsubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        var state = accessor.getHeader(SUBSCRIPTION_STATE);
        if (state != null && subscriptionBySession.remove(accessor.getSessionId(), state)) {
            broadcast();
        }
    }

    /** 소켓 연결이 끊기면 그 세션을 집계에서 빼고 인원수를 다시 방송한다. */
    @EventListener
    public synchronized void onDisconnect(SessionDisconnectEvent event) {
        var attributes =
                SimpMessageHeaderAccessor.getSessionAttributes(event.getMessage().getHeaders());
        if (attributes != null) {
            // 대기 중인 SUBSCRIBE도 같은 세션 속성을 공유한다. 종료 표식은 세션과 함께 해제된다.
            // 표식 확인·추가·제거를 같은 락으로 묶어 disconnect와 구독 완료 간 경합을 막는다.
            attributes.put(DISCONNECTED, true);
            attributes.remove(SUBSCRIPTIONS);
        }
        if (subscriptionBySession.remove(event.getSessionId()) != null) {
            broadcast();
        }
    }

    private void broadcast() {
        messagingTemplate.convertAndSend(
                DESTINATION, new ChatPresenceResponse(subscriptionBySession.size()));
    }

    private static final class SessionSubscriptions {
        private final Map<String, SubscriptionState> byId = new HashMap<>();
    }

    private static final class SubscriptionState {
        private boolean active = true;
    }
}
