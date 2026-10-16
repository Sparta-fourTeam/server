package com.novaserver.upgrade.controller;

import com.novaserver.upgrade.dto.UpgradeResponse;
import com.novaserver.upgrade.service.UpgradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 강화 조회 API. */
@Tag(name = "강화 조회", description = "강화 조회")
@RestController
@RequestMapping("/upgrade")
@RequiredArgsConstructor
public class UpgradeController {
    private final UpgradeService upgradeService;

    @Operation(summary = "(유니티) 강화 데이터 조회", description = "강화 데이터를 전체 조회합니다.")
    @GetMapping
    public List<UpgradeResponse> getUpgradeData() {
        return upgradeService.getUpgradeList();
    }
}
