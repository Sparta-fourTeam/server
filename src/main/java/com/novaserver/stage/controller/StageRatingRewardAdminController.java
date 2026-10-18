package com.novaserver.stage.controller;

import com.novaserver.stage.dto.StageRatingRewardRequest;
import com.novaserver.stage.dto.StageRatingRewardResponse;
import com.novaserver.stage.service.StageRatingRewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 스테이지 별등급 보상 관리자용 API. */
@Tag(name = "스테이지 별등급 보상 관리", description = "별등급 최초 클리어 보상 등록·수정·삭제")
@RestController
@RequestMapping("/admin/stage-rating-reward")
@RequiredArgsConstructor
@Validated
public class StageRatingRewardAdminController {
    private final StageRatingRewardService stageRatingRewardService;

    @Operation(summary = "별등급 보상 목록 조회", description = "해당 스테이지의 별등급 보상 목록을 조회합니다.")
    @GetMapping("/{stageId}/list")
    public List<StageRatingRewardResponse> getStageRatingRewards(@PathVariable Integer stageId) {
        return stageRatingRewardService.getStageRatingRewardList(stageId);
    }

    @Operation(summary = "별등급 보상 상세 조회", description = "없는 조합이면 404를 반환합니다.")
    @GetMapping("/{stageId}/{rating}")
    public StageRatingRewardResponse getStageRatingReward(
            @PathVariable Integer stageId, @PathVariable @Min(1) @Max(3) int rating) {
        return stageRatingRewardService.getStageRatingReward(stageId, rating);
    }

    @Operation(
            summary = "별등급 보상 등록",
            description =
                    "없는 스테이지 id면 404, 이미 등록되어 있으면 409를 반환합니다. rating은 1~3입니다. "
                            + "rewardDefinitionId는 필수입니다. 별도로 등록한 고정 보상 묶음을 연결합니다.")
    @PostMapping("/{stageId}/{rating}")
    public StageRatingRewardResponse createStageRatingReward(
            @PathVariable Integer stageId,
            @PathVariable @Min(1) @Max(3) int rating,
            @Valid @RequestBody StageRatingRewardRequest request) {
        return stageRatingRewardService.createStageRatingReward(stageId, rating, request);
    }

    @Operation(
            summary = "별등급 보상 수정",
            description =
                    "없는 조합이면 404를 반환합니다. rating은 1~3입니다. "
                            + "rewardDefinitionId는 필수입니다. 별도로 등록한 고정 보상 묶음을 연결합니다.")
    @PutMapping("/{stageId}/{rating}")
    public StageRatingRewardResponse updateStageRatingReward(
            @PathVariable Integer stageId,
            @PathVariable @Min(1) @Max(3) int rating,
            @Valid @RequestBody StageRatingRewardRequest request) {
        return stageRatingRewardService.updateStageRatingReward(stageId, rating, request);
    }

    @Operation(summary = "별등급 보상 삭제", description = "별등급 보상을 삭제합니다.")
    @DeleteMapping("/{stageId}/{rating}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStageRatingReward(
            @PathVariable Integer stageId, @PathVariable @Min(1) @Max(3) int rating) {
        stageRatingRewardService.deleteStageRatingReward(stageId, rating);
    }
}
