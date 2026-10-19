package com.novaserver.battle.progress;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 웨이브 완료·정상 나가기 시 누적 보고. 결과 보상을 지급하지 않는다. */
@RestController
@RequiredArgsConstructor
@RequestMapping
public class BattleProgressController {
    private final BattleProgressService progress;

    @PostMapping("/battle/progress/open")
    public BattleProgressService.State open(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody Open request) {
        return progress.open(
                userId, request.battleId(), request.stageId(), request.sourceRevision());
    }

    @PostMapping("/battle/progress")
    public ProgressAck report(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody ProgressReport request) {
        return progress.report(userId, request);
    }

    @PostMapping("/battle/progress/abandon")
    public ProgressAck abandon(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody ProgressReport request) {
        return progress.abandon(userId, request);
    }

    @GetMapping("/battle/progress/{battleId}")
    public BattleProgressService.State state(
            @AuthenticationPrincipal Long userId, @PathVariable String battleId) {
        return progress.snapshot(userId, battleId);
    }

    /** 실제 발급받은 활성 전투의 최초 스테이지 고정 입력. */
    public record Open(
            @NotBlank String battleId,
            @NotNull Integer stageId,
            @NotNull @Pattern(regexp = "[0-9a-f]{64}") String sourceRevision) {}
}
