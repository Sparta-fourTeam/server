package com.novaserver.chat.controller;

import com.novaserver.chat.dto.ChatMessageRequest;
import com.novaserver.chat.dto.ChatMessageResponse;
import com.novaserver.chat.service.ChatService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

/** 전체 채팅 WebSocket 핸들러. */
@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @MessageMapping("/chat.send")
    @SendTo("/topic/chat")
    public ChatMessageResponse send(ChatMessageRequest request, Principal principal) {
        Long senderId = (Long) ((Authentication) principal).getPrincipal();
        return chatService.createMessage(senderId, request);
    }
}
