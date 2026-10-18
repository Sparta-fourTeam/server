package com.novaserver.shop.controller;

import com.novaserver.shop.dto.ShopConfigurationRequest;
import com.novaserver.shop.dto.ShopConfigurationResponse;
import com.novaserver.shop.service.ShopConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 일일 상점 서버 정책 등록·조회 API. */
@RestController
@RequestMapping("/admin/shop")
@RequiredArgsConstructor
public class ShopAdminController {
    private final ShopConfigurationService service;

    @GetMapping
    public ShopConfigurationResponse get() {
        return service.get();
    }

    @Operation(
            summary = "상점 정책 전체 교체",
            description =
                    "최초 expectedVersion=0, 이후 조회한 버전이 필요합니다. 기존 공통 "
                            + "보상 묶음을 참조합니다. 현재 계정의 오늘 진열·가격·재추첨 정책은 유지하고 이후 "
                            + "새 일일 진열에 반영합니다. 광고 한도는 보관하나 검증 공급자가 없어 사용 불가입니다.")
    @PutMapping
    public ShopConfigurationResponse replace(@Valid @RequestBody ShopConfigurationRequest r) {
        return service.replace(r);
    }
}
