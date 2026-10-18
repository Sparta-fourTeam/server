package com.novaserver.shop.controller;

import com.novaserver.shop.dto.GoldPackBuyRequest;
import com.novaserver.shop.dto.GoldPackCatalogResponse;
import com.novaserver.shop.dto.GoldPackReceiptResponse;
import com.novaserver.shop.service.UserGoldPackService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 보유 잼으로 구매하는 골드팩 조회·구매 API. 현금 잼 결제를 성공 처리하지 않는다. */
@RestController
@RequestMapping("/user/shop/gold-packs")
@RequiredArgsConstructor
public class UserGoldPackController {
    private final UserGoldPackService service;

    @Operation(
            summary = "골드팩 교환 가격 조회",
            description =
                    "서버 카탈로그의 version, index, 지급 gold, "
                            + "discountPercent, gemPrice를 함께 조회합니다. 미설정이면 "
                            + "available=false입니다. 구매 한도는 기존 Local처럼 별도로 두지 않습니다.")
    @GetMapping
    public GoldPackCatalogResponse get(@AuthenticationPrincipal Long userId) {
        return service.catalog(userId);
    }

    @Operation(
            summary = "잼으로 골드팩 구매",
            description =
                    "requestId·조회한 configVersion·index만 받으며 가격·골드 "
                            + "입력은 거절합니다. 버전이 바뀐 새 요청은 409입니다. 일일 구매·재추첨과 요청 "
                            + "키를 공유해 종류 변경도 409입니다. 같은 키는 날짜·설정 변경 뒤에도 최초 "
                            + "영수증을 반환합니다. 차감·공통 골드 지급·영수증은 함께 저장/롤백하며 현재 "
                            + "잔액은 user/me로 복원합니다. IAP나 현금 잼 상품은 이 경로에 없습니다.")
    @PostMapping("/buy")
    public GoldPackReceiptResponse buy(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody GoldPackBuyRequest r) {
        return service.buy(userId, r.getRequestId(), r.getConfigVersion(), r.getIndex());
    }
}
