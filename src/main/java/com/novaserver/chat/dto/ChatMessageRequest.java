package com.novaserver.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** 전체 채팅 전송 요청. */
@Getter
@Schema(description = "전체 채팅 전송 요청")
public class ChatMessageRequest {
    @Schema(
            description = "보낼 메시지 내용",
            example = "안녕하세요",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "내용은 필수입니다")
    private String content;
}
