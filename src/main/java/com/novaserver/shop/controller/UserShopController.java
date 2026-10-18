package com.novaserver.shop.controller;

import com.novaserver.shop.dto.ShopBuyRequest;
import com.novaserver.shop.dto.ShopReceiptResponse;
import com.novaserver.shop.dto.ShopRerollRequest;
import com.novaserver.shop.dto.ShopStateResponse;
import com.novaserver.shop.service.UserShopService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 서버가 확정한 진열 복원과 검증 가능한 일일 상품 구매·골드 재추첨 API. */
@RestController
@RequestMapping("/user/shop")
@RequiredArgsConstructor
public class UserShopController {
    private final UserShopService service;

    @Operation(
            summary = "현재 상점 복원",
            description =
                    "KST04 날짜, UTC 초기화 시각, 회차·품절·골드 재추첨 잔여와 실제 "
                            + "상품·할인·가격을 반환합니다. 서버가 최초 추첨을 저장하며 앱은 Local 난수를 "
                            + "재계산하지 않습니다. 미설정이면 available=false, 광고는 adAvailable=false입니다.")
    @GetMapping
    public ShopStateResponse get(@AuthenticationPrincipal Long userId) {
        return service.snapshot(userId);
    }

    @Operation(
            summary = "진열 상품 구매",
            description =
                    "조회한 dayKey·rerollIndex·slot과 새 requestId만 "
                            + "받습니다. 가격·보상 입력은 거절합니다. 오래된 날짜·회차는 409입니다. 슬롯은 "
                            + "회차당 한 번만 구매합니다. 같은 요청 키는 날짜/회차 경계 뒤에도 최초 실제 "
                            + "보상과 당시 상태를 돌려주며 현재 상태는 다시 조회합니다.")
    @PostMapping("/buy")
    public ShopReceiptResponse buy(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody ShopBuyRequest r) {
        return service.buy(
                userId, r.getRequestId(), r.getDayKey(), r.getRerollIndex(), r.getSlot());
    }

    @Operation(
            summary = "골드 재추첨",
            description =
                    "GOLD만 지원하며 서버 동결 가격·한도를 검사합니다. 새 회차에서 품절 슬롯을 "
                            + "비우며 첫 칸은 다시 무료입니다. 차감·횟수·실제 새 진열·영수증은 함께 "
                            + "저장/롤백합니다. AD는 검증 공급자가 없어 503입니다. 현금 잼 결제와 골드팩 교환은 이 API 범위에 없습니다.")
    @PostMapping("/reroll")
    public ShopReceiptResponse reroll(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody ShopRerollRequest r) {
        return service.reroll(
                userId, r.getRequestId(), r.getDayKey(), r.getRerollIndex(), r.getKind());
    }
}
