package com.novaserver.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 전체 채팅방에 방송되는 메시지. */
public record ChatMessageResponse(
        Long senderId,
        String content,
        @Schema(description = "서버 UTC 전송 시각(ISO8601 Z)") Instant sentAt) {}
