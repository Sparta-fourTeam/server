package com.novaserver.chat.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.novaserver.chat.dto.ChatPresenceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@ExtendWith(MockitoExtension.class)
class ChatPresenceServiceTest {

    private static final String DESTINATION = "/topic/chat/presence";
    private static final String OTHER_DESTINATION = "/topic/chat";

    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private ChatPresenceService service;

    @Test
    @DisplayName("presence 구독이 늘어날 때마다 현재 인원수를 방송한다")
    void broadcastsCountOnEachSubscribe() {
        service.onSubscribe(subscribeEvent("session-1", "sub-1", DESTINATION));
        service.onSubscribe(subscribeEvent("session-2", "sub-1", DESTINATION));

        verify(messagingTemplate).convertAndSend(DESTINATION, new ChatPresenceResponse(1));
        verify(messagingTemplate).convertAndSend(DESTINATION, new ChatPresenceResponse(2));
    }

    @Test
    @DisplayName("presence가 아닌 destination 구독은 집계하지 않는다")
    void ignoresSubscriptionsToOtherDestinations() {
        service.onSubscribe(subscribeEvent("session-1", "sub-1", OTHER_DESTINATION));

        verify(messagingTemplate, never()).convertAndSend(DESTINATION, new ChatPresenceResponse(1));
    }

    @Test
    @DisplayName("같은 세션이 두 번 구독해도 한 번만 센다")
    void countsOneSessionOnce() {
        service.onSubscribe(subscribeEvent("session-1", "sub-1", DESTINATION));
        service.onSubscribe(subscribeEvent("session-1", "sub-2", DESTINATION));

        verify(messagingTemplate, times(2))
                .convertAndSend(DESTINATION, new ChatPresenceResponse(1));
        verify(messagingTemplate, never()).convertAndSend(DESTINATION, new ChatPresenceResponse(2));
    }

    @Test
    @DisplayName("본인 구독을 해제하면 인원수가 줄어든다")
    void decrementsOnUnsubscribe() {
        service.onSubscribe(subscribeEvent("session-1", "sub-1", DESTINATION));
        service.onSubscribe(subscribeEvent("session-2", "sub-1", DESTINATION));
        service.onUnsubscribe(unsubscribeEvent("session-1", "sub-1"));

        verify(messagingTemplate).convertAndSend(DESTINATION, new ChatPresenceResponse(2));
        verify(messagingTemplate, times(2))
                .convertAndSend(DESTINATION, new ChatPresenceResponse(1));
    }

    @Test
    @DisplayName("presence와 무관한 구독을 해제하면 인원수를 건드리지 않는다")
    void ignoresUnsubscribeForUnrelatedSubscription() {
        service.onSubscribe(subscribeEvent("session-1", "sub-1", DESTINATION));
        service.onUnsubscribe(unsubscribeEvent("session-1", "sub-other"));

        verify(messagingTemplate, never()).convertAndSend(DESTINATION, new ChatPresenceResponse(0));
    }

    @Test
    @DisplayName("연결이 끊기면 본인 구독이 집계에서 빠진다")
    void decrementsOnDisconnect() {
        service.onSubscribe(subscribeEvent("session-1", "sub-1", DESTINATION));
        service.onSubscribe(subscribeEvent("session-2", "sub-1", DESTINATION));
        service.onDisconnect(disconnectEvent("session-1"));

        verify(messagingTemplate).convertAndSend(DESTINATION, new ChatPresenceResponse(2));
        verify(messagingTemplate, times(2))
                .convertAndSend(DESTINATION, new ChatPresenceResponse(1));
    }

    @Test
    @DisplayName("presence를 구독하지 않은 세션이 끊겨도 방송하지 않는다")
    void ignoresDisconnectForUntrackedSession() {
        service.onDisconnect(disconnectEvent("session-never-subscribed"));

        verify(messagingTemplate, never()).convertAndSend(DESTINATION, new ChatPresenceResponse(0));
    }

    private static SessionSubscribeEvent subscribeEvent(
            String sessionId, String subscriptionId, String destination) {
        SimpMessageHeaderAccessor accessor =
                SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setDestination(destination);
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionSubscribeEvent(new Object(), message);
    }

    private static SessionUnsubscribeEvent unsubscribeEvent(
            String sessionId, String subscriptionId) {
        SimpMessageHeaderAccessor accessor =
                SimpMessageHeaderAccessor.create(SimpMessageType.UNSUBSCRIBE);
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionUnsubscribeEvent(new Object(), message);
    }

    private static SessionDisconnectEvent disconnectEvent(String sessionId) {
        SimpMessageHeaderAccessor accessor =
                SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        accessor.setSessionId(sessionId);
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(new Object(), message, sessionId, CloseStatus.NORMAL);
    }
}
