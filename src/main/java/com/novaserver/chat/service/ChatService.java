package com.novaserver.chat.service;

import com.novaserver.chat.dto.ChatMessageRequest;
import com.novaserver.chat.dto.ChatMessageResponse;
import com.novaserver.chat.entity.ChatMessage;
import com.novaserver.chat.repository.ChatMessageRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 전체 채팅 메시지 저장·조회. */
@Service
@RequiredArgsConstructor
public class ChatService {
    private static final int RECENT_MESSAGE_LIMIT = 30;

    private final ChatMessageRepository chatMessageRepository;

    /** 메시지를 저장하고 방송용 응답으로 변환한다. */
    @Transactional
    public ChatMessageResponse createMessage(Long senderId, ChatMessageRequest request) {
        ChatMessage saved =
                chatMessageRepository.save(new ChatMessage(senderId, request.getContent()));
        return ChatMessageResponse.from(saved);
    }

    /** 최근 메시지를 오래된 순으로 돌려준다. 이후 메시지는 WebSocket으로 받는다. */
    public List<ChatMessageResponse> getRecentMessages() {
        List<ChatMessage> recent =
                chatMessageRepository.findAllByOrderByCreatedAtDesc(
                        PageRequest.of(0, RECENT_MESSAGE_LIMIT));
        Collections.reverse(recent);
        return recent.stream().map(ChatMessageResponse::from).toList();
    }
}
