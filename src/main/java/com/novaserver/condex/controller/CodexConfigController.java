package com.novaserver.condex.controller;

import com.novaserver.condex.dto.CodexGradeRewardRequest;
import com.novaserver.condex.dto.CodexGradeRewardResponse;
import com.novaserver.condex.dto.CodexMonsterRequest;
import com.novaserver.condex.dto.CodexMonsterResponse;
import com.novaserver.condex.service.CodexConfigService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** WEB 관리자만 도감 카탈로그·등급별 공통 보상 연결을 관리한다. 발견 등록 API가 아니다. */
@RestController
@RequestMapping("/admin/codex")
@RequiredArgsConstructor
public class CodexConfigController {
    private final CodexConfigService service;

    @GetMapping("/monsters")
    public List<CodexMonsterResponse> monsters() {
        return service.monsters();
    }

    @Operation(
            summary = "도감용 몬스터 ID·등급 등록/교체",
            description =
                    "전체 몬스터 능력치 API가 아닙니다. "
                            + "양수 int ID와 Normal/Elite/Boss를 명시합니다. 생성은 expectedVersion 생략, "
                            + "교체는 현재 버전 필수입니다. 계정 발견 상태를 등록하지 않습니다.")
    @PutMapping("/monsters/{monsterId}")
    public CodexMonsterResponse saveMonster(
            @PathVariable Long monsterId, @Valid @RequestBody CodexMonsterRequest request) {
        return service.saveMonster(monsterId, request);
    }

    @GetMapping("/rewards")
    public List<CodexGradeRewardResponse> rewards() {
        return service.rewards();
    }

    @Operation(
            summary = "등급별 공통 골드 보상 연결",
            description =
                    "Normal/Elite/Boss에 기존 보상 ID를 연결합니다. "
                            + "양수 골드·아이템 없는 묶음만 허용합니다. 생성은 버전 생략, 교체는 현재 버전 필수, "
                            + "불일치는409입니다. 운영 기본값·자동 seed가 없으며 미설정 등급의 수령은503입니다. "
                            + "클라이언트 조우 보고를 인정하거나 신규 발견을 자동 연결하지 않습니다.")
    @PutMapping("/rewards/{grade}")
    public CodexGradeRewardResponse saveReward(
            @PathVariable String grade, @Valid @RequestBody CodexGradeRewardRequest request) {
        return service.saveReward(grade, request);
    }

    @Operation(summary = "등급별 공급 중단", description = "신규 수령은503, 기존 영수증 재전송은 유지됩니다.")
    @DeleteMapping("/rewards/{grade}")
    public void deleteReward(@PathVariable String grade) {
        service.deleteReward(grade);
    }
}
