package com.novaserver.notice.dto;

import com.novaserver.notice.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** SSE로 방송되거나 목록 조회로 내려가는 공지. */
public record NoticeResponse(
        Long id,
        String title,
        String content,
        @Schema(description = "서버 UTC 등록 시각(ISO8601 Z)") Instant createdAt) {
    /** 저장된 공지를 응답으로 변환한다. */
    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(), notice.getTitle(), notice.getContent(), notice.getCreatedAt());
    }
}
