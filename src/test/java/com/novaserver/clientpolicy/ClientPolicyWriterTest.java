package com.novaserver.clientpolicy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.novaserver.auth.jwt.GameWebSocketSessionRegistry;
import com.novaserver.chat.repository.ChatMessageRepository;
import com.novaserver.chat.service.ChatPresenceService;
import com.novaserver.chat.service.ChatService;
import com.novaserver.global.config.WebSocketConfig;
import com.novaserver.mail.repository.MailRepository;
import com.novaserver.mail.service.MailService;
import com.novaserver.notice.repository.NoticeRepository;
import com.novaserver.notice.service.NoticeService;
import com.novaserver.version.config.DataVersionInitializer;
import com.novaserver.version.repository.DataVersionRepository;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.socket.WebSocketHandler;

class ClientPolicyWriterTest {
    @Test
    void establishedSocketRechecksMinimumUsingHandshakeMetadataInsteadOfNativeHeaders() {
        ClientPolicySource policy = mock(ClientPolicySource.class);
        final var inbound = inbound(policy);
        Map<String, Object> attributes = new HashMap<>();
        var request = new MockHttpServletRequest();
        request.addHeader("X-Game-Platform", "android");
        request.addHeader("X-Game-Build", "10");
        new ClientPolicyHandshakeInterceptor()
                .beforeHandshake(
                        new ServletServerHttpRequest(request),
                        new ServletServerHttpResponse(new MockHttpServletResponse()),
                        mock(WebSocketHandler.class),
                        attributes);
        var send =
                MessageBuilder.withPayload("synthetic chat")
                        .setHeader(
                                SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER,
                                SimpMessageType.MESSAGE)
                        .setHeader(SimpMessageHeaderAccessor.SESSION_ATTRIBUTES, attributes)
                        .setHeader("nativeHeaders", Map.of("X-Game-Build", List.of("999")))
                        .build();
        when(policy.current()).thenReturn(ClientPolicy.disabled());
        assertThatCode(() -> inbound.preSend(send, null)).doesNotThrowAnyException();
        when(policy.current()).thenReturn(minimumPolicy("NORMAL", "11", true));
        assertThatThrownBy(() -> inbound.preSend(send, null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("CLIENT_UPDATE_REQUIRED");
        when(policy.current()).thenReturn(minimumPolicy("DRAINING", "10", true));
        assertThatCode(() -> inbound.preSend(send, null)).doesNotThrowAnyException();
        when(policy.current()).thenReturn(ClientPolicy.disabled());
        assertThatCode(() -> inbound.preSend(send, null)).doesNotThrowAnyException();
    }

    @Test
    void establishedLegacyAndInvalidBuildsFollowTheCurrentCompatibilityPolicy() {
        ClientPolicySource policy = mock(ClientPolicySource.class);
        var inbound = inbound(policy);
        var legacy =
                MessageBuilder.withPayload("test")
                        .setHeader(
                                SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER,
                                SimpMessageType.MESSAGE)
                        .build();
        when(policy.current()).thenReturn(minimumPolicy("NORMAL", "11", true));
        assertThatCode(() -> inbound.preSend(legacy, null)).doesNotThrowAnyException();
        when(policy.current()).thenReturn(minimumPolicy("NORMAL", "11", false));
        assertThatThrownBy(() -> inbound.preSend(legacy, null))
                .hasMessageContaining("CLIENT_UPDATE_REQUIRED");
        for (String build : new String[] {"bad", "10", "11"}) {
            var send =
                    MessageBuilder.withPayload("test")
                            .setHeader(
                                    SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER,
                                    SimpMessageType.MESSAGE)
                            .setHeader(
                                    SimpMessageHeaderAccessor.SESSION_ATTRIBUTES,
                                    Map.of(
                                            ClientPolicyHandshakeInterceptor.IDENTITY_ATTRIBUTE,
                                            new ClientPolicyHandshakeInterceptor.Identity(
                                                    "android", build)))
                            .build();
            if (build.equals("11")) {
                assertThatCode(() -> inbound.preSend(send, null)).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> inbound.preSend(send, null))
                        .hasMessageContaining("CLIENT_UPDATE_REQUIRED");
            }
        }
    }

    private ClientPolicy minimumPolicy(String mode, String minimum, boolean allowLegacy) {
        return new ClientPolicy(
                1,
                "synthetic",
                true,
                mode,
                "test",
                null,
                30,
                allowLegacy,
                Map.of(
                        "android",
                        new ClientPolicy.PlatformPolicy(
                                minimum,
                                minimum,
                                "https://play.google.com/store/apps/details?id=test")));
    }

    @SuppressWarnings("unchecked")
    private ChannelInterceptor inbound(ClientPolicySource policy) {
        ObjectProvider<ChatPresenceService> presence = mock(ObjectProvider.class);
        var service = mock(ChatPresenceService.class);
        when(presence.getObject()).thenReturn(service);
        when(service.prepareMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ObjectProvider<ClientPolicySource> policies = mock(ObjectProvider.class);
        when(policies.getIfAvailable()).thenReturn(policy);
        var registration = mock(ChannelRegistration.class);
        new WebSocketConfig(presence, policies, mock(GameWebSocketSessionRegistry.class))
                .configureClientInboundChannel(registration);
        ArgumentCaptor<ChannelInterceptor[]> captor =
                ArgumentCaptor.forClass(ChannelInterceptor[].class);
        verify(registration).interceptors(captor.capture());
        return captor.getValue()[0];
    }

    @Test
    void maintenanceSkipsScheduledDeletesAndApplicationInitializer() {
        ClientPolicySource policy = mock(ClientPolicySource.class);
        when(policy.current()).thenReturn(ClientPolicy.unavailable());
        NoticeRepository notices = mock(NoticeRepository.class);
        MailRepository mails = mock(MailRepository.class);
        ChatMessageRepository messages = mock(ChatMessageRepository.class);
        DataVersionRepository versions = mock(DataVersionRepository.class);
        NoticeService notice = new NoticeService(notices, Clock.systemUTC());
        MailService mail = new MailService(mails, null, null, null, Clock.systemUTC());
        ChatService chat = new ChatService(messages, Clock.systemUTC(), null);
        DataVersionInitializer initializer =
                new DataVersionInitializer(versions, mock(PlatformTransactionManager.class));
        for (Object service : new Object[] {notice, mail, chat, initializer}) {
            ReflectionTestUtils.setField(service, "clientPolicy", policy);
        }
        notice.deleteOldNotices();
        mail.deleteOldMails();
        chat.deleteOldMessages();
        initializer.run(null);
        verifyNoInteractions(notices, mails, messages, versions);
    }

    @Test
    @SuppressWarnings("unchecked")
    void maintenanceRejectsSendOnAlreadyEstablishedWebSocket() {
        ObjectProvider<ChatPresenceService> presence = mock(ObjectProvider.class);
        ObjectProvider<ClientPolicySource> policies = mock(ObjectProvider.class);
        ClientPolicySource source = mock(ClientPolicySource.class);
        when(policies.getIfAvailable()).thenReturn(source);
        when(source.current()).thenReturn(ClientPolicy.unavailable());
        WebSocketConfig config =
                new WebSocketConfig(presence, policies, mock(GameWebSocketSessionRegistry.class));
        ChannelRegistration registration = mock(ChannelRegistration.class);
        config.configureClientInboundChannel(registration);
        ArgumentCaptor<ChannelInterceptor[]> captor =
                ArgumentCaptor.forClass(ChannelInterceptor[].class);
        verify(registration).interceptors(captor.capture());
        var message =
                MessageBuilder.withPayload("test")
                        .setHeader(
                                SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER,
                                SimpMessageType.MESSAGE)
                        .build();
        assertThatThrownBy(() -> captor.getValue()[0].preSend(message, null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("MAINTENANCE");
        verifyNoInteractions(presence);
    }
}
