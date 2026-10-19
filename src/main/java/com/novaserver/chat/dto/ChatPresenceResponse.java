package com.novaserver.chat.dto;

/** 현재 /topic/chat/presence 구독자 수. */
public record ChatPresenceResponse(int onlineCount) {}
