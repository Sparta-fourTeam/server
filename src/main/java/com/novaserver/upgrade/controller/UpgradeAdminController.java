package com.novaserver.upgrade.controller;

import com.novaserver.upgrade.dto.UpgradeCreateRequest;
import com.novaserver.upgrade.dto.UpgradeRequest;
import com.novaserver.upgrade.dto.UpgradeResponse;
import com.novaserver.upgrade.service.UpgradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 강화 관리 관리자용 API. */
@Tag(name = "강화 관리", description = "강화 등록·수정·삭제")
@RestController
@RequestMapping("/admin/upgrade")
@RequiredArgsConstructor
public class UpgradeAdminController {
    private final UpgradeService upgradeService;

    @Operation(summary = "강화 목록 조회", description = "강화 목록을 조회합니다.")
    @GetMapping("/list")
    public List<UpgradeResponse> getUpgrades() {
        return upgradeService.getUpgradeList();
    }

    @Operation(summary = "강화 상세 조회", description = "해당 강화 정보를 조회합니다.")
    @GetMapping("/{id}")
    public UpgradeResponse getUpgrade(@PathVariable String id) {
        return upgradeService.getUpgrade(id);
    }

    @Operation(summary = "강화 등록", description = "강화 정보를 등록합니다.")
    @PostMapping
    public UpgradeResponse createUpgrade(@Valid @RequestBody UpgradeCreateRequest request) {
        return upgradeService.createUpgrade(request);
    }

    @Operation(summary = "강화 수정", description = "강화를 수정합니다.")
    @PutMapping("/{id}")
    public UpgradeResponse updateUpgrade(
            @PathVariable String id, @Valid @RequestBody UpgradeRequest request) {
        return upgradeService.updateUpgrade(id, request);
    }

    @Operation(summary = "강화 삭제", description = "강화를 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUpgrade(@PathVariable String id) {
        upgradeService.deleteUpgrade(id);
    }
}
