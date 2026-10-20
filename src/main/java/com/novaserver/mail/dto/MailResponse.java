package com.novaserver.mail.dto;

import com.novaserver.mail.entity.Mail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** SSE로 전송되거나 목록 조회로 내려가는 우편. */
public record MailResponse(
        Long id,
        @Schema(description = "받는 유저 id") Long userId,
        String title,
        String content,
        int exp,
        int gold,
        int gem,
        int energy,
        boolean claimed,
        List<MailItemResponse> items,
        @Schema(description = "서버 UTC 등록 시각(ISO8601 Z)") Instant createdAt) {
    /** 저장된 우편을 응답으로 변환한다. */
    public static MailResponse from(Mail mail) {
        return new MailResponse(
                mail.getId(),
                mail.getUser().getId(),
                mail.getTitle(),
                mail.getContent(),
                mail.getExp(),
                mail.getGold(),
                mail.getGem(),
                mail.getEnergy(),
                mail.isClaimed(),
                mail.getItems().stream().map(MailItemResponse::from).toList(),
                mail.getCreatedAt());
    }
}
