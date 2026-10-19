package com.novaserver.notice.controller;

import com.novaserver.notice.dto.NoticeRequest;
import com.novaserver.notice.dto.NoticeResponse;
import com.novaserver.notice.service.NoticeService;
import com.novaserver.notification.service.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** WEB 관리자 전용 공지 등록 API. */
@Tag(name = "공지", description = "관리자 공지 등록 API")
@RestController
@RequestMapping("/admin/notices")
@RequiredArgsConstructor
public class NoticeAdminController {
    private static final String EVENT_NAME = "notice";

    private final NoticeService noticeService;
    private final SseEmitterService sseEmitterService;

    @Operation(summary = "공지 등록", description = "공지를 저장하고 접속 중인 모든 유저에게 SSE(notice 이벤트)로 즉시 방송합니다.")
    @PostMapping
    public NoticeResponse create(@Valid @RequestBody NoticeRequest request) {
        // noticeService.createNotice()의 트랜잭션이 커밋된 뒤(메서드 반환 후)에 방송해야, 방송을 보고 바로
        // GET /notices/recent를 호출해도 커밋 전이라 목록에 없는 레이스가 생기지 않는다.
        NoticeResponse response = noticeService.createNotice(request);
        sseEmitterService.broadcast(EVENT_NAME, response);
        return response;
    }
}
