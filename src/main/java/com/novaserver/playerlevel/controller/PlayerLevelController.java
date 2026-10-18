package com.novaserver.playerlevel.controller;

import com.novaserver.playerlevel.dto.PlayerLevelConfigResponse;
import com.novaserver.playerlevel.service.PlayerLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게임 클라이언트용 플레이어 경험치 기준 조회 API. */
@Tag(name = "플레이어 레벨")
@RestController
@RequestMapping("/player-levels")
@RequiredArgsConstructor
public class PlayerLevelController {
    private final PlayerLevelService service;

    @Operation(
            summary = "플레이어 레벨 기준 조회",
            description =
                    "DB 기준과 PlayerLevels 버전을 반환합니다. " + "미등록 상태는 503이며 서버 모드에서 로컬 기준으로 대체하지 않습니다.")
    @GetMapping
    public PlayerLevelConfigResponse getConfig() {
        return service.getConfig();
    }
}
