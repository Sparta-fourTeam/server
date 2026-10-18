package com.novaserver.condex.controller;

import com.novaserver.condex.dto.CodexClaimRequest;
import com.novaserver.condex.dto.CodexReceiptResponse;
import com.novaserver.condex.dto.CodexStateResponse;
import com.novaserver.condex.service.UserCodexService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 계정 도감 복원·최초 골드 수령 API. 발견 등록 HTTP 경로는 제공하지 않는다. */
@RestController
@RequestMapping("/user/codex")
@RequiredArgsConstructor
public class UserCodexController {
    private final UserCodexService service;

    @Operation(summary = "도감 발견·수령 상태 복원", description = "인증 계정의 최초 발견과 수령 영수증만 반환합니다.")
    @GetMapping
    public List<CodexStateResponse> list(@AuthenticationPrincipal Long userId) {
        return service.list(userId);
    }

    @Operation(
            summary = "몬스터 도감 보상 수령",
            description =
                    "몬스터 ID가 계정별 자연 멱등 키입니다. "
                            + "최초 지급과 수령 저장은 한 트랜잭션이며 중복·동시 요청은 최초 영수증을 반환합니다. "
                            + "미발견은 409, 없는 몬스터는 404, #50 등급별 골드 미설정은 503입니다. "
                            + "현재 골드는 user/me로 조회합니다. 서버 검증된 조우 이벤트만 내부 등록 가능하며 "
                            + "현재 전투 조우 검증은 미구현이므로 클라이언트 보고 등록을 연결하지 않습니다.")
    @PostMapping("/claim")
    public CodexReceiptResponse claim(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody CodexClaimRequest request) {
        return service.claim(userId, request.monsterId());
    }
}
