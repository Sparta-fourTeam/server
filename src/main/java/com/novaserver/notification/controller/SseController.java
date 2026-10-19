package com.novaserver.notification.controller;

import com.novaserver.notification.service.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** SSE 구독 API. 공지·우편함 등 서버 푸시 기능이 이 연결을 통해 이벤트를 전송한다. */
@Tag(name = "알림", description = "SSE 구독 API")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class SseController {
    private final SseEmitterService sseEmitterService;

    @Operation(summary = "SSE 구독", description = "토큰의 유저 id로 SSE 연결을 맺는다. 연결 직후 connect 이벤트를 보낸다.")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal Long userId) {
        return sseEmitterService.subscribe(userId);
    }
}
