package com.novaserver.quest.controller;

import com.novaserver.quest.dto.QuestBoardResponse;
import com.novaserver.quest.dto.QuestClaimRequest;
import com.novaserver.quest.dto.QuestReceiptResponse;
import com.novaserver.quest.service.UserQuestService;
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

/** 계정별 퀘스트 진행 복원과 기간을 지정한 점수·상자 수령 API. */
@RestController
@RequestMapping("/user/quests")
@RequiredArgsConstructor
public class UserQuestController {
    private final UserQuestService service;

    @Operation(
            summary = "현재 일일·주간 퀘스트 복원",
            description =
                    "서버 KST04 영업일과 월요일 04시 주간 기준. 시각은 UTC. 로그인과 실제 전투 입장 "
                            + "에너지 소모와 스폰 범위 검사를 통과한 누적 보고의 처치 차이만 집계합니다.")
    @GetMapping
    public List<QuestBoardResponse> list(@AuthenticationPrincipal Long userId) {
        return service.snapshot(userId);
    }

    @Operation(
            summary = "완료 퀘스트 점수 수령",
            description =
                    "조회한 id, period, periodKey만 받습니다. 임의 count·보상은 거절합니다. "
                            + "목표는 서버 행동 집계로 검증합니다. 같은 계정+기간+ID 재전송은 최초 영수증을"
                            + " 반환합니다. 미수령 과거 기간 요청은 409이며 새 기간을 수령하지 않습니다.")
    @PostMapping("/claim")
    public QuestReceiptResponse claim(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody QuestClaimRequest r) {
        return service.claim(userId, r.getId(), r.getPeriod(), r.getPeriodKey(), false);
    }

    @Operation(
            summary = "수령 점수로 상자 보상 수령",
            description =
                    "완료 수치가 아닌 수령한 퀘스트 점수 합을 검증합니다. 공통 고정 골드·아이템, 에너지·젬과 "
                            + "영수증을 함께 확정/롤백합니다. 재전송은 기간 경계를 지나도 최초 지급 결과만 "
                            + "반환합니다. 현재 잔액은 user/me로 조회합니다.")
    @PostMapping("/chests/claim")
    public QuestReceiptResponse chest(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody QuestClaimRequest r) {
        return service.claim(userId, r.getId(), r.getPeriod(), r.getPeriodKey(), true);
    }
}
