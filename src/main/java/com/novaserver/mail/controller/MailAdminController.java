package com.novaserver.mail.controller;

import com.novaserver.mail.dto.MailBroadcastRequest;
import com.novaserver.mail.dto.MailBulkRequest;
import com.novaserver.mail.dto.MailRequest;
import com.novaserver.mail.dto.MailResponse;
import com.novaserver.mail.dto.MailSendCountResponse;
import com.novaserver.mail.service.MailService;
import com.novaserver.notification.service.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** WEB 관리자 전용 우편 발송·조회 API. */
@Tag(name = "우편함", description = "관리자 우편 발송·조회 API")
@RestController
@RequestMapping("/admin/mails")
@RequiredArgsConstructor
public class MailAdminController {
    private static final String EVENT_NAME = "mail";

    private final MailService mailService;
    private final SseEmitterService sseEmitterService;

    @Operation(
            summary = "우편 발송",
            description =
                    "특정 유저에게 우편을 저장하고, 접속 중이면 SSE(mail 이벤트)로 제목·내용·경험치·골드·젬·에너지·첨부 아이템 목록을 즉시"
                            + " 전송합니다. 우편은 30일 지나면 수령 여부와 상관없이 자동으로 삭제됩니다.")
    @PostMapping
    public MailResponse send(@Valid @RequestBody MailRequest request) {
        // mailService.sendMail()의 트랜잭션이 커밋된 뒤(메서드 반환 후)에 전송해야, 전송을 보고 바로
        // GET /mails를 호출해도 커밋 전이라 목록에 없는 레이스가 생기지 않는다.
        MailResponse response = mailService.sendMail(request);
        sseEmitterService.sendTo(request.getUserId(), EVENT_NAME, response);
        return response;
    }

    @Operation(
            summary = "우편 대량 발송",
            description =
                    "지정한 유저 목록에 같은 우편을 저장하고, 접속 중인 유저에게는 SSE(mail 이벤트)로 즉시 전송합니다. "
                            + "존재하지 않는 userId는 조용히 건너뛰며, sentCount는 실제로 저장된 건수입니다.")
    @PostMapping("/bulk")
    public MailSendCountResponse sendBulk(@Valid @RequestBody MailBulkRequest request) {
        List<MailResponse> sent = mailService.sendBulkMail(request);
        sent.forEach(response -> sseEmitterService.sendTo(response.userId(), EVENT_NAME, response));
        return new MailSendCountResponse(sent.size());
    }

    @Operation(
            summary = "우편 전체 발송",
            description = "전체 유저에게 같은 우편을 저장하고, 접속 중인 유저에게는 SSE(mail 이벤트)로 즉시 전송합니다.")
    @PostMapping("/broadcast")
    public MailSendCountResponse sendBroadcast(@Valid @RequestBody MailBroadcastRequest request) {
        List<MailResponse> sent = mailService.sendBroadcastMail(request);
        sent.forEach(response -> sseEmitterService.sendTo(response.userId(), EVENT_NAME, response));
        return new MailSendCountResponse(sent.size());
    }

    @Operation(summary = "유저 우편함 조회", description = "특정 유저의 우편함을 최신순으로 조회합니다.")
    @GetMapping
    public List<MailResponse> getMails(@RequestParam Long userId) {
        return mailService.getMails(userId);
    }
}
