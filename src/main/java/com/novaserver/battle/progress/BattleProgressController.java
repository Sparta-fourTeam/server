package com.novaserver.battle.progress;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 웨이브 완료·정상 나가기 시 누적 보고. 결과 보상을 지급하지 않는다. */
@RestController
@RequiredArgsConstructor
@RequestMapping
public class BattleProgressController {
    private final BattleProgressService progress;
    private final BattleProgressPolicyService policies;

    @PostMapping("/battle/progress/open")
    public BattleProgressService.State open(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody Open request) {
        return progress.open(userId, request.battleId(), request.stageId());
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

    @PutMapping("/admin/stages/{stageId}/progress-policy")
    public PolicyVersion policy(
            @PathVariable Integer stageId, @RequestBody StageProgressPolicy policy) {
        var saved = policies.save(stageId, policy);
        return new PolicyVersion(stageId, saved.getVersion());
    }

    /** 실제 발급받은 활성 전투의 최초 스테이지 고정 입력. */
    public record Open(@NotBlank String battleId, @NotNull Integer stageId) {}

    /** 관리자 정책 등록의 확정 개정. 운영 데이터는 자동 생성하지 않는다. */
    public record PolicyVersion(Integer stageId, int version) {}
}
