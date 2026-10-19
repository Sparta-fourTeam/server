package com.novaserver.chat.service;

import com.novaserver.chat.dto.ChatMessageRequest;
import com.novaserver.chat.dto.ChatMessageResponse;
import com.novaserver.chat.entity.ChatMessage;
import com.novaserver.chat.repository.ChatMessageRepository;
import com.novaserver.user.dto.UserProfileResponse;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.service.UserProfileService;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 전체 채팅 메시지 저장·조회·정리. */
@Service
@RequiredArgsConstructor
public class ChatService {
    private static final int RECENT_MESSAGE_LIMIT = 30;
    private static final int RETENTION_DAYS = 30;

    private final ChatMessageRepository chatMessageRepository;
    private final Clock clock;
    private final UserProfileService profiles;
    private final UserRepository users;

    /** 메시지를 저장하고 방송용 응답으로 변환한다. */
    @Transactional
    public ChatMessageResponse createMessage(Long senderId, ChatMessageRequest request) {
        var profile = profiles.getProfile(senderId);
        ChatMessage saved =
                chatMessageRepository.save(new ChatMessage(senderId, request.getContent()));
        return ChatMessageResponse.from(saved, profile);
    }

    /** 최근 메시지를 오래된 순으로 돌려준다. 이후 메시지는 WebSocket으로 받는다. */
    public List<ChatMessageResponse> getRecentMessages() {
        List<ChatMessage> recent =
                chatMessageRepository.findAllByOrderByCreatedAtDesc(
                        PageRequest.of(0, RECENT_MESSAGE_LIMIT));
        Collections.reverse(recent);
        var senderIds = recent.stream().map(ChatMessage::getSenderId).distinct().toList();
        var identities =
                users.findAllById(senderIds).stream()
                        .map(UserProfileResponse::from)
                        .collect(
                                Collectors.toMap(UserProfileResponse::userId, Function.identity()));
        return recent.stream()
                .map(
                        message ->
                                ChatMessageResponse.from(
                                        message,
                                        identities.getOrDefault(
                                                message.getSenderId(),
                                                new UserProfileResponse(
                                                        message.getSenderId(), "탈퇴한 플레이어", 0))))
                .toList();
    }

    /** 보관 기한이 지난 메시지를 매일 새벽에 지운다. */
    @Scheduled(cron = "0 0 4 * * *", zone = "UTC")
    @Transactional
    public void deleteOldMessages() {
        chatMessageRepository.deleteByCreatedAtBefore(
                clock.instant().minus(RETENTION_DAYS, ChronoUnit.DAYS));
    }
}
