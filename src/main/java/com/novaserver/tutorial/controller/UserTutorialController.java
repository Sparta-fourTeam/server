package com.novaserver.tutorial.controller;

import com.novaserver.tutorial.dto.TutorialCompleteRequest;
import com.novaserver.tutorial.dto.TutorialReceiptResponse;
import com.novaserver.tutorial.dto.TutorialStateResponse;
import com.novaserver.tutorial.service.UserTutorialService;
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

/** 계정별 튜토리얼 상태 복원과 최초 완료·시청 기록 API. */
@RestController
@RequestMapping("/user/tutorials")
@RequiredArgsConstructor
public class UserTutorialController {
    private final UserTutorialService service;

    @GetMapping
    public List<TutorialStateResponse> list(@AuthenticationPrincipal Long userId) {
        return service.list(userId);
    }

    @Operation(
            summary = "튜토리얼 완료 또는 도입 대사 시청 기록",
            description =
                    "문자열 tutorialId만 받습니다. .dialogue는 알려진 기본 튜토리얼의 시청 기록이며 "
                            + "조건·보상 없이 저장합니다. 본 완료는 서버 조건을 검증하고 고정 보상 지급과 함께 "
                            + "확정합니다. 계정+ID별 재전송은 최초 영수증을 반환하고 다시 지급하지 않습니다. "
                            + "현재 재화는 user/me로 조회합니다. 대사 본문과 번역은 저장하지 않습니다.")
    @PostMapping("/complete")
    public TutorialReceiptResponse complete(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TutorialCompleteRequest request) {
        return service.complete(userId, request.getTutorialId());
    }
}
