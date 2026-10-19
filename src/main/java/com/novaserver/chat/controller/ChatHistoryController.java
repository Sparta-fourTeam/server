package com.novaserver.chat.controller;

import com.novaserver.chat.dto.ChatMessageResponse;
import com.novaserver.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 전체 채팅 기록 조회 API. */
@Tag(name = "채팅", description = "전체 채팅 기록 조회 API")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatHistoryController {
    private final ChatService chatService;

    @Operation(
            summary = "최근 채팅 조회",
            description = "가장 최근 30개를 오래된 순으로 반환합니다. 이후 실시간 메시지는 WebSocket(/topic/chat)으로 받습니다.")
    @GetMapping("/recent")
    public List<ChatMessageResponse> getRecentMessages() {
        return chatService.getRecentMessages();
    }
}
