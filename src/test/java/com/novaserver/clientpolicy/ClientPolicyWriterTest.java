package com.novaserver.clientpolicy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

class ClientPolicyWriterTest {
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
        DataVersionInitializer initializer = new DataVersionInitializer(versions);
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
