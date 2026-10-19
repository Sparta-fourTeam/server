package com.novaserver.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.novaserver.chat.dto.ChatMessageRequest;
import com.novaserver.chat.entity.ChatMessage;
import com.novaserver.chat.repository.ChatMessageRepository;
import com.novaserver.global.config.ClockConfig;
import com.novaserver.global.config.JpaAuditingConfig;
import com.novaserver.user.Provider;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.service.UserProfileService;
import jakarta.persistence.EntityManager;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({ChatService.class, UserProfileService.class, ClockConfig.class, JpaAuditingConfig.class})
class ChatServiceTest {
    @Autowired private ChatService chat;
    @Autowired private ChatMessageRepository messages;
    @Autowired private UserRepository users;
    @Autowired private UserProfileService profiles;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    @Test
    void historyInitializesAndPersistsLegacyIdentityBeforeProfileAndLiveMessage() {
        User sender = new User(Provider.GUEST, "legacy-chat-sender");
        sender.updateProfile(null, 3);
        Long senderId = users.saveAndFlush(sender).getId();
        messages.saveAndFlush(new ChatMessage(senderId, "old message"));
        messages.saveAndFlush(new ChatMessage(senderId, "another old message"));
        jdbc.update("update users set nickname = null, nickname_key = null where id = ?", senderId);
        entityManager.clear();

        var history = chat.getRecentMessages();

        assertThat(history).hasSize(2);
        String nickname = history.getFirst().nickname();
        assertThat(history)
                .allSatisfy(message -> assertThat(message.nickname()).isEqualTo(nickname));
        assertThat(history).allSatisfy(message -> assertThat(message.avatarId()).isEqualTo(3));
        entityManager.flush();
        entityManager.clear();
        User stored = users.findById(senderId).orElseThrow();
        assertThat(stored.getNickname()).isEqualTo(nickname);
        assertThat(stored.getNicknameKey()).isEqualTo(nickname.toLowerCase(Locale.ROOT));
        assertThat(profiles.getProfile(senderId).nickname()).isEqualTo(nickname);
        var request = new ChatMessageRequest();
        ReflectionTestUtils.setField(request, "content", "new live message");
        assertThat(chat.createMessage(senderId, request).nickname()).isEqualTo(nickname);
        assertThat(chat.getRecentMessages())
                .allSatisfy(message -> assertThat(message.nickname()).isEqualTo(nickname));
    }

    @Test
    void historyKeepsExistingIdentityAndDeletedSenderFallback() {
        User sender = new User(Provider.GUEST, "existing-chat-sender");
        sender.updateProfile("ExistingHero", 4);
        Long senderId = users.saveAndFlush(sender).getId();
        messages.saveAndFlush(new ChatMessage(senderId, "existing sender"));
        messages.saveAndFlush(new ChatMessage(Long.MAX_VALUE, "deleted sender"));
        entityManager.clear();

        var history = chat.getRecentMessages();

        assertThat(history).hasSize(2);
        var existing =
                history.stream()
                        .filter(message -> message.senderId().equals(senderId))
                        .findFirst()
                        .orElseThrow();
        assertThat(existing.nickname()).isEqualTo("ExistingHero");
        assertThat(existing.avatarId()).isEqualTo(4);
        var deleted =
                history.stream()
                        .filter(message -> message.senderId().equals(Long.MAX_VALUE))
                        .findFirst()
                        .orElseThrow();
        assertThat(deleted.nickname()).isEqualTo("탈퇴한 플레이어");
        assertThat(deleted.avatarId()).isZero();
    }

    @Test
    void emptyHistoryRemainsEmpty() {
        assertThat(chat.getRecentMessages()).isEmpty();
    }
}
