package com.novaserver.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.novaserver.auth.jwt.GameWebSocketSessionRegistry;
import com.novaserver.chat.dto.ChatPresenceResponse;
import com.novaserver.global.config.WebSocketConfig;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
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

    private final Map<String, Map<String, Object>> attributesBySession = new ConcurrentHashMap<>();

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

    @Test
    @DisplayName("구독 처리 전에 종료된 세션은 지연 구독으로 다시 집계하지 않는다")
    void ignoresSubscribeAfterDisconnect() {
        var delayed = subscribeEvent("closed-session", "presence", DESTINATION);
        service.onDisconnect(disconnectEvent("closed-session"));
        service.onDisconnect(disconnectEvent("closed-session"));

        service.onSubscribe(delayed);

        verifyNoInteractions(messagingTemplate);
        service.onSubscribe(subscribeEvent("live-session", "presence", DESTINATION));
        verify(messagingTemplate).convertAndSend(DESTINATION, new ChatPresenceResponse(1));
    }

    @Test
    @DisplayName("기존 구독을 종료한 뒤 지연된 재구독도 집계하지 않는다")
    void ignoresResubscribeAfterDisconnect() {
        service.onSubscribe(subscribeEvent("closed-session", "presence", DESTINATION));
        service.onDisconnect(disconnectEvent("closed-session"));

        service.onSubscribe(subscribeEvent("closed-session", "late-presence", DESTINATION));
        service.onSubscribe(subscribeEvent("live-session", "presence", DESTINATION));

        verify(messagingTemplate).convertAndSend(DESTINATION, new ChatPresenceResponse(0));
        verify(messagingTemplate, times(2))
                .convertAndSend(DESTINATION, new ChatPresenceResponse(1));
        verify(messagingTemplate, never()).convertAndSend(DESTINATION, new ChatPresenceResponse(2));
    }

    @Test
    @DisplayName("구독 완료 전에 해제된 구독은 지연 콜백으로 다시 집계하지 않는다")
    void ignoresSubscribeAfterUnsubscribe() {
        var delayed = subscribeEvent("session-1", "presence", DESTINATION);
        service.onUnsubscribe(unsubscribeEvent("session-1", "presence"));

        service.onSubscribe(delayed);

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("구독 ID를 재사용해도 이전 구독의 지연 해제가 새 구독을 제거하지 않는다")
    void staleUnsubscribeDoesNotRemoveReusedSubscriptionId() {
        var oldSubscribe = subscribeEvent("session-1", "presence", DESTINATION);
        var oldUnsubscribe = unsubscribeEvent("session-1", "presence");
        var newSubscribe = subscribeEvent("session-1", "presence", DESTINATION);
        service.onSubscribe(newSubscribe);

        service.onUnsubscribe(oldUnsubscribe);
        service.onSubscribe(oldSubscribe);

        verify(messagingTemplate).convertAndSend(DESTINATION, new ChatPresenceResponse(1));
        verify(messagingTemplate, never()).convertAndSend(DESTINATION, new ChatPresenceResponse(0));
    }

    @Test
    @DisplayName("실제 브로커의 대기 중 구독이 연결 종료 뒤 실행되어도 집계에서 제외한다")
    void queuedSubscribeAfterDisconnectDoesNotInflateActualBrokerCount()
            throws InterruptedException {
        assertQueuedSubscribeNotCounted(true);
    }

    @Test
    @DisplayName("실제 브로커의 구독 해제 뒤 지연 구독 콜백이 실행되어도 집계에서 제외한다")
    void queuedSubscribeAfterUnsubscribeDoesNotInflateActualBrokerCount()
            throws InterruptedException {
        assertQueuedSubscribeNotCounted(false);
    }

    private void assertQueuedSubscribeNotCounted(boolean disconnect) throws InterruptedException {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(
                    WebSocketConfig.class,
                    ChatPresenceService.class,
                    GameWebSocketSessionRegistry.class);
            context.refresh();
            var inbound =
                    context.getBean("clientInboundChannel", ExecutorSubscribableChannel.class);
            var outbound =
                    context.getBean("clientOutboundChannel", ExecutorSubscribableChannel.class);
            var received = new LinkedBlockingQueue<Message<?>>();
            outbound.subscribe(received::add);
            var subscribeQueued = new CountDownLatch(1);
            var resumeSubscribe = new CountDownLatch(1);
            var subscribeHandled = new CountDownLatch(1);
            var unsubscribeHandled = new CountDownLatch(1);
            var interceptors = new ArrayList<>(inbound.getInterceptors());
            interceptors.add(
                    0,
                    new ExecutorChannelInterceptor() {
                        @Override
                        public Message<?> beforeHandle(
                                Message<?> message,
                                MessageChannel channel,
                                MessageHandler handler) {
                            if (isDelayedSubscribe(message, handler)) {
                                subscribeQueued.countDown();
                                try {
                                    if (!resumeSubscribe.await(5, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException(
                                                "SUBSCRIBE was not released");
                                    }
                                } catch (InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                    throw new IllegalStateException(ex);
                                }
                            }
                            return message;
                        }

                        @Override
                        public void afterMessageHandled(
                                Message<?> message,
                                MessageChannel channel,
                                MessageHandler handler,
                                Exception error) {
                            if (isDelayedSubscribe(message, handler)) {
                                subscribeHandled.countDown();
                            }
                            if (handler instanceof SimpleBrokerMessageHandler
                                    && SimpMessageHeaderAccessor.getMessageType(
                                                    message.getHeaders())
                                            == SimpMessageType.UNSUBSCRIBE) {
                                unsubscribeHandled.countDown();
                            }
                        }

                        private boolean isDelayedSubscribe(
                                Message<?> message, MessageHandler handler) {
                            return handler instanceof SimpleBrokerMessageHandler
                                    && SimpMessageHeaderAccessor.getMessageType(
                                                    message.getHeaders())
                                            == SimpMessageType.SUBSCRIBE
                                    && "closed-session"
                                            .equals(
                                                    SimpMessageHeaderAccessor.getSessionId(
                                                            message.getHeaders()));
                        }
                    });
            inbound.setInterceptors(interceptors);
            try {
                inbound.send(subscribeMessage("closed-session", "presence", DESTINATION));
                assertThat(subscribeQueued.await(3, TimeUnit.SECONDS)).isTrue();
                if (disconnect) {
                    context.publishEvent(disconnectEvent("closed-session"));
                } else {
                    inbound.send(unsubscribeMessage("closed-session", "presence"));
                    assertThat(unsubscribeHandled.await(3, TimeUnit.SECONDS)).isTrue();
                }
            } finally {
                resumeSubscribe.countDown();
            }
            assertThat(subscribeHandled.await(3, TimeUnit.SECONDS)).isTrue();

            var connect = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT);
            connect.setSessionId("live-session");
            connect.setSessionAttributes(attributes("live-session"));
            inbound.send(MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders()));
            assertThat(received.poll(3, TimeUnit.SECONDS)).isNotNull();
            inbound.send(subscribeMessage("live-session", "presence", DESTINATION));

            var count = received.poll(3, TimeUnit.SECONDS);
            assertThat(count).isNotNull();
            assertThat(SimpMessageHeaderAccessor.getSessionId(count.getHeaders()))
                    .isEqualTo("live-session");
            assertThat(new String((byte[]) count.getPayload(), StandardCharsets.UTF_8))
                    .contains("\"onlineCount\":1");
        }
    }

    @Test
    @DisplayName("첫 구독자 혼자 있어도 구독 직후 현재 인원수를 받는다")
    void firstSubscriberReceivesInitialCountThroughActualBroker() throws InterruptedException {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(
                    WebSocketConfig.class,
                    ChatPresenceService.class,
                    GameWebSocketSessionRegistry.class);
            context.refresh();
            var outbound =
                    context.getBean("clientOutboundChannel", ExecutorSubscribableChannel.class);
            final var inbound =
                    context.getBean("clientInboundChannel", ExecutorSubscribableChannel.class);
            var received = new LinkedBlockingQueue<Message<?>>();
            outbound.subscribe(received::add);
            var connect = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT);
            connect.setSessionId("only-session");
            connect.setSessionAttributes(attributes("only-session"));
            inbound.send(MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders()));
            assertThat(received.poll(3, TimeUnit.SECONDS)).isNotNull();

            inbound.send(subscribeMessage("only-session", "presence", DESTINATION));

            var initial = received.poll(3, TimeUnit.SECONDS);
            assertThat(initial).isNotNull();
            assertThat(SimpMessageHeaderAccessor.getDestination(initial.getHeaders()))
                    .isEqualTo(DESTINATION);
            assertThat(SimpMessageHeaderAccessor.getSubscriptionId(initial.getHeaders()))
                    .isEqualTo("presence");
            String json = new String((byte[]) initial.getPayload(), StandardCharsets.UTF_8);
            assertThat(json).contains("\"onlineCount\":1");
        }
    }

    private SessionSubscribeEvent subscribeEvent(
            String sessionId, String subscriptionId, String destination) {
        return new SessionSubscribeEvent(
                new Object(),
                service.prepareMessage(subscribeMessage(sessionId, subscriptionId, destination)));
    }

    private Message<byte[]> subscribeMessage(
            String sessionId, String subscriptionId, String destination) {
        SimpMessageHeaderAccessor accessor =
                SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setDestination(destination);
        accessor.setSessionAttributes(attributes(sessionId));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private SessionUnsubscribeEvent unsubscribeEvent(String sessionId, String subscriptionId) {
        return new SessionUnsubscribeEvent(
                new Object(),
                service.prepareMessage(unsubscribeMessage(sessionId, subscriptionId)));
    }

    private Message<byte[]> unsubscribeMessage(String sessionId, String subscriptionId) {
        SimpMessageHeaderAccessor accessor =
                SimpMessageHeaderAccessor.create(SimpMessageType.UNSUBSCRIBE);
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setSessionAttributes(attributes(sessionId));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private SessionDisconnectEvent disconnectEvent(String sessionId) {
        SimpMessageHeaderAccessor accessor =
                SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        accessor.setSessionId(sessionId);
        accessor.setSessionAttributes(attributes(sessionId));
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(new Object(), message, sessionId, CloseStatus.NORMAL);
    }

    private Map<String, Object> attributes(String sessionId) {
        return attributesBySession.computeIfAbsent(sessionId, ignored -> new HashMap<>());
    }
}
