package com.novaserver.reward.controller;

import com.novaserver.reward.dto.RewardDefinitionRequest;
import com.novaserver.reward.dto.RewardDefinitionResponse;
import com.novaserver.reward.service.RewardDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 전용 공통 고정 보상 묶음 API. */
@RestController
@RequestMapping("/admin/reward-definitions")
@RequiredArgsConstructor
public class RewardDefinitionController {
    private final RewardDefinitionService service;

    @Operation(summary = "고정 보상 묶음 목록", description = "실제 아이템·수량과 버전·별등급·튜토리얼·퀘스트·도감 참조 수를 조회합니다.")
    @GetMapping
    public List<RewardDefinitionResponse> list() {
        return service.list();
    }

    @Operation(summary = "고정 보상 묶음 조회")
    @GetMapping("/{id}")
    public RewardDefinitionResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(
            summary = "고정 보상 묶음 등록",
            description = "실제 Item 참조와 양수 수량을 검증합니다. 랜덤 추첨·상자 개봉은 수행하지 않습니다.")
    @PostMapping
    public RewardDefinitionResponse create(@Valid @RequestBody RewardDefinitionRequest request) {
        return service.create(request);
    }

    @Operation(
            summary = "공유 고정 보상 묶음 전체 교체",
            description =
                    "현재 expectedVersion 필수. 불일치는 409입니다. "
                            + "모든 참조의 미래 수령에 반영되며 과거 영수증은 바뀌지 않습니다. 도감 참조 중에는 양수 골드 전용만 허용합니다.")
    @PutMapping("/{id}")
    public RewardDefinitionResponse update(
            @PathVariable Long id, @Valid @RequestBody RewardDefinitionRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "고정 보상 묶음 삭제", description = "별등급·튜토리얼·퀘스트·도감이 참조 중이면 409로 거절합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
