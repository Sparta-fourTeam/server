package com.novaserver.user.controller;

import com.novaserver.user.dto.EnergyPurchaseStateResponse;
import com.novaserver.user.dto.EnergyRecoveryRequest;
import com.novaserver.user.dto.EnergyRecoveryResponse;
import com.novaserver.user.service.UserEnergyRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 인증 계정의 추가 에너지 회복 API. */
@Tag(name = "유저")
@RestController
@RequestMapping("/user/energy")
@RequiredArgsConstructor
public class UserEnergyRecoveryController {
    private final UserEnergyRecoveryService service;

    @Operation(
            summary = "현재 에너지 구매 상태 조회",
            description =
                    "JWT 계정의 현재 영업일 구매/잔여 횟수와 gem 가격·지급량·하루 한도를 조회합니다. "
                            + "한국시간 04시 기준으로 이전 기간은 0회로 표시하며 조회는 재화·횟수·영수증을 변경하지 않습니다. "
                            + "serverTime과 resetsAt은 UTC Z, purchaseDay는 서울 영업일입니다. "
                            + "복구한 최초 영수증의 과거 purchaseCount 대신 이 응답으로 현재 한도를 표시하세요. "
                            + "현재 잔액은 GET /api/user/me로 별도 조회합니다. 구매 시 서버가 잔액과 일일 한도를 다시 검증합니다.")
    @GetMapping("/purchase-state")
    public EnergyPurchaseStateResponse getPurchaseState(@AuthenticationPrincipal Long userId) {
        return service.getPurchaseState(userId);
    }

    @Operation(
            summary = "추가 에너지 회복",
            description =
                    "Purchase는 gem 60 차감 후 에너지 15 지급, 한국시간 매일 04시 초기화·하루 6회. "
                            + "가격·수량·유저ID는 요청으로 받지 않습니다. 자연회복 정산 후 상한30 초과 허용. "
                            + "잔액/한도/저장 범위 초과는409이며 차감하지 않습니다. Ad는 검증 미연결503. "
                            + "같은 계정/createId/경로는 날짜 변경 후에도 최초 영수증 반환, 경로 변경409. "
                            + "최초 확정 시각은 UTC Z, 계정별 기준 날짜·구매 횟수는 별도 저장하며 새 기간은 0회부터 계산. "
                            + "응답은 최초 확정 결과이며 현재 잔액은 GET /api/user/me, "
                            + "현재 구매 횟수/정책은 GET /api/user/energy/purchase-state로 조회합니다.")
    @PostMapping("/recover")
    public EnergyRecoveryResponse recover(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody EnergyRecoveryRequest request) {
        return service.recover(userId, request);
    }
}
