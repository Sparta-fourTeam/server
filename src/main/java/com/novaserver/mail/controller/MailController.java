package com.novaserver.mail.controller;

import com.novaserver.mail.dto.MailResponse;
import com.novaserver.mail.service.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 우편함 조회·수령 API. */
@Tag(name = "우편함", description = "우편함 조회·수령 API")
@RestController
@RequestMapping("/mails")
@RequiredArgsConstructor
public class MailController {
    private final MailService mailService;

    @Operation(
            summary = "우편함 조회",
            description = "내 우편을 최신순으로 반환합니다. 접속 중에는 SSE(mail 이벤트)로 실시간으로 받습니다.")
    @GetMapping
    public List<MailResponse> getMails(@AuthenticationPrincipal Long userId) {
        return mailService.getMails(userId);
    }

    @Operation(summary = "우편 수령", description = "첨부된 아이템을 인벤토리로 지급받습니다. 이미 수령한 우편은 다시 수령할 수 없습니다.")
    @PostMapping("/{mailId}/claim")
    public MailResponse claim(@AuthenticationPrincipal Long userId, @PathVariable Long mailId) {
        return mailService.claimMail(userId, mailId);
    }
}
