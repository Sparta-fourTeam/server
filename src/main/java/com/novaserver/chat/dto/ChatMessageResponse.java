package com.novaserver.chat.dto;

import com.novaserver.chat.entity.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 전체 채팅방에 방송되는 메시지. */
public record ChatMessageResponse(
        Long senderId,
        String content,
        @Schema(description = "서버 UTC 전송 시각(ISO8601 Z)") Instant sentAt) {
    /** 저장된 메시지를 응답으로 변환한다. */
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getSenderId(), message.getContent(), message.getCreatedAt());
    }
}
