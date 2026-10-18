package com.novaserver.shop.controller;

import com.novaserver.shop.dto.GoldPackCatalogResponse;
import com.novaserver.shop.dto.GoldPackConfigurationRequest;
import com.novaserver.shop.service.GoldPackConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 게임 재화 교환 카탈로그 API. 현금 상품 설정과 무관하다. */
@RestController
@RequestMapping("/admin/shop/gold-packs")
@RequiredArgsConstructor
public class GoldPackAdminController {
    private final GoldPackConfigurationService service;

    @GetMapping
    public GoldPackCatalogResponse get() {
        return service.get();
    }

    @Operation(
            summary = "잼→골드 카탈로그 전체 교체",
            description =
                    "최초 expectedVersion=0, 이후 조회한 버전을 보냅니다. "
                            + "gemGoldRate는 양수, 골드는 양수·오름차순, 할인율은 "
                            + "0~99·비감소입니다. 클라 공식의 정수 내림 가격을 적용하며 새 일일 한도나 "
                            + "최소 가격을 만들지 않습니다. 출시값은 자동 입력하지 않습니다.")
    @PutMapping
    public GoldPackCatalogResponse replace(@RequestBody GoldPackConfigurationRequest r) {
        return service.replace(r);
    }
}
