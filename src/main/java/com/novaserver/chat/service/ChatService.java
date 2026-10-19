package com.novaserver.chat.service;

import com.novaserver.chat.dto.ChatMessageRequest;
import com.novaserver.chat.dto.ChatMessageResponse;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 전체 채팅 메시지 처리. */
@Service
@RequiredArgsConstructor
public class ChatService {
    private final Clock clock;

    /** 보낸 사람과 서버 시각을 채워 방송용 메시지를 만든다. */
    public ChatMessageResponse createMessage(Long senderId, ChatMessageRequest request) {
        return new ChatMessageResponse(senderId, request.getContent(), clock.instant());
    }
}
