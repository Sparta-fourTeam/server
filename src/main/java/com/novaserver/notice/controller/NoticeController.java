package com.novaserver.notice.controller;

import com.novaserver.notice.dto.NoticeResponse;
import com.novaserver.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공지 조회 API. */
@Tag(name = "공지", description = "공지 조회 API")
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeService noticeService;

    @Operation(
            summary = "최근 공지 조회",
            description = "가장 최근 20개를 최신순으로 반환합니다. 접속 중에는 SSE(notice 이벤트)로 실시간으로 받습니다.")
    @GetMapping("/recent")
    public List<NoticeResponse> getRecentNotices() {
        return noticeService.getRecentNotices();
    }
}
