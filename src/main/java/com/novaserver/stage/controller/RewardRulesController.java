package com.novaserver.stage.controller;

import com.novaserver.stage.dto.RewardRulesData;
import com.novaserver.stage.dto.RewardRulesResponse;
import com.novaserver.stage.service.RewardRulesService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 보상 기준 조회와 관리자 교체 API. */
@RestController
@RequiredArgsConstructor
public class RewardRulesController {
    private final RewardRulesService service;

    @Operation(
            summary = "공통 보상 기준 조회",
            description = "일정과 itemId별 분류를 같은 RewardRules 버전으로 반환. 미설정/잘못된 기준은 503")
    @GetMapping("/reward-rules")
    public RewardRulesResponse getConfig() {
        return service.getConfig();
    }

    @Operation(
            summary = "공통 보상 기준 등록·교체",
            description =
                    "모든 일정과 아이템 분류를 명시. 저장과 RewardRules/revision 증가를 함께 처리. 기존 스테이지 총량과 충돌하면 거절")
    @PutMapping("/admin/reward-rules")
    public RewardRulesResponse saveConfig(@Valid @RequestBody RewardRulesData request) {
        return service.saveConfig(request);
    }
}
