package com.novaserver.battle.controller;

import com.novaserver.battle.dto.BattleRequest;
import com.novaserver.battle.dto.BattleResponse;
import com.novaserver.battle.dto.BattleStartRequest;
import com.novaserver.battle.service.BattleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 게임 배틀 관련 API. */
@Tag(name = "배틀", description = "배틀 ID 발급 및 종료 API")
@RestController
@RequestMapping("/battle")
@RequiredArgsConstructor
public class BattleController {
    private final BattleService battleService;

    @Operation(
            summary = "배틀 id 발급",
            description = "유저에게 새 배틀 ID를 발급합니다. 진행 중인 배틀이 있으면 새 ID로 덮어씁니다.")
    @PostMapping("/start")
    public BattleResponse startBattle(@Valid @RequestBody BattleStartRequest request) {
        // TODO: JWT 적용 후 토큰에서 userId를 꺼내도록 변경
        return battleService.createBattle(request.getUserId());
    }

    @Operation(summary = "배틀 종료", description = "배틀 ID가 일치하면 삭제하고, 다르면 에러를 반환합니다.")
    @PostMapping("/end")
    public void endBattle(@Valid @RequestBody BattleRequest request) {
        // TODO: JWT 적용 후 토큰에서 userId를 꺼내도록 변경
        battleService.endBattle(request.getUserId(), request.getBattleId());
    }
}
