package com.novaserver.playerlevel.controller;

import com.novaserver.playerlevel.dto.PlayerLevelConfigRequest;
import com.novaserver.playerlevel.dto.PlayerLevelConfigResponse;
import com.novaserver.playerlevel.service.PlayerLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자용 플레이어 경험치 기준 등록·교체 API. */
@Tag(name = "플레이어 레벨 관리")
@RestController
@RequestMapping("/admin/player-levels")
@RequiredArgsConstructor
public class PlayerLevelAdminController {
    private final PlayerLevelService service;

    @Operation(
            summary = "플레이어 레벨 기준 등록·교체",
            description =
                    "baseExp·stepExp·maxLevel 모두 필수. "
                            + "기준 저장과 PlayerLevels/전체 revision 증가를 한 트랜잭션으로 처리합니다.")
    @PutMapping
    public PlayerLevelConfigResponse saveConfig(
            @Valid @RequestBody PlayerLevelConfigRequest request) {
        return service.saveConfig(request);
    }
}
