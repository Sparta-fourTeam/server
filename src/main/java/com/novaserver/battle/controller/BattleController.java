package com.novaserver.battle.controller;

import com.novaserver.battle.dto.BattleEndRequest;
import com.novaserver.battle.dto.BattleRequest;
import com.novaserver.battle.dto.BattleResponse;
import com.novaserver.battle.dto.BattleStartRequest;
import com.novaserver.battle.service.BattleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게임 배틀 관련 API. */
@Tag(name = "배틀", description = "배틀 ID 발급 및 종료 API")
@RestController
@RequestMapping("/battle")
@RequiredArgsConstructor
public class BattleController {
    private final BattleService battleService;

    @Operation(
            summary = "배틀 ID 발급",
            description =
                    "토큰의 유저 id로 새 배틀 ID를 발급합니다. 같은 멱등성 키로 재요청하면 기존 배틀 ID를 그대로 돌려줍니다. "
                            + "새 요청이고 진행 중인 배틀이 있으면 새 ID로 덮어씁니다.")
    @PostMapping("/start")
    public BattleResponse startBattle(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody BattleStartRequest request) {
        return battleService.createBattle(userId, request.getCreateId());
    }

    @Operation(
            summary = "배틀 포기",
            description = "배틀 ID가 일치하면 삭제합니다. 불일치 시 400, 진행 중인 배틀이 없으면 404를 반환합니다.")
    @PostMapping("/cancel")
    public void cancelBattle(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody BattleRequest request) {
        battleService.cancelBattle(userId, request.getBattleId());
    }

    @Operation(
            summary = "배틀 종료",
            description =
                    "배틀 ID가 일치하면 클리어한 웨이브·등급에 따라 기본 보상을 지급하고 스테이지 진행도를 갱신한 뒤 배틀을 삭제합니다. "
                            + "불일치 시 400, 진행 중인 배틀이 없으면 404를 반환합니다.")
    @PostMapping("/end")
    public void endBattle(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody BattleEndRequest request) {
        battleService.endBattle(userId, request);
    }
}
