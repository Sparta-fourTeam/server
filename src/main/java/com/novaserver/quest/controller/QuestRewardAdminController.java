package com.novaserver.quest.controller;

import com.novaserver.quest.dto.QuestRewardCreateRequest;
import com.novaserver.quest.dto.QuestRewardRequest;
import com.novaserver.quest.dto.QuestRewardResponse;
import com.novaserver.quest.service.QuestRewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/** 퀘스트 보상 관리 관리자용 API. */
@Tag(name = "퀘스트 보상 관리", description = "누적 포인트 보상 등록·수정·삭제")
@RestController
@RequestMapping("/admin/quest-reward")
@RequiredArgsConstructor
public class QuestRewardAdminController {
    private final QuestRewardService questRewardService;

    @Operation(summary = "보상 목록 조회", description = "등록된 모든 보상을 조회합니다.")
    @GetMapping("/list")
    public List<QuestRewardResponse> getQuestRewards() {
        return questRewardService.getQuestRewardList();
    }

    @Operation(summary = "보상 상세 조회", description = "해당 보상 정보를 조회합니다.")
    @GetMapping("/{rewardId}")
    public QuestRewardResponse getQuestReward(@PathVariable String rewardId) {
        return questRewardService.getQuestReward(rewardId);
    }

    @Operation(summary = "보상 등록", description = "보상을 등록합니다.")
    @PostMapping
    public QuestRewardResponse createQuestReward(
            @Valid @RequestBody QuestRewardCreateRequest request) {
        return questRewardService.createQuestReward(request);
    }

    @Operation(summary = "보상 수정", description = "없는 id면 404를 반환합니다.")
    @PutMapping("/{rewardId}")
    public QuestRewardResponse updateQuestReward(
            @PathVariable String rewardId, @Valid @RequestBody QuestRewardRequest request) {
        return questRewardService.updateQuestReward(rewardId, request);
    }

    @Operation(summary = "보상 삭제", description = "보상을 삭제합니다.")
    @DeleteMapping("/{rewardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestReward(@PathVariable String rewardId) {
        questRewardService.deleteQuestReward(rewardId);
    }
}
