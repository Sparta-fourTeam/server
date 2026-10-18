package com.novaserver.user.controller;

import com.novaserver.user.dto.StageRatingRewardClaimRequest;
import com.novaserver.user.dto.UserDataResponse;
import com.novaserver.user.dto.UserStageProgressResponse;
import com.novaserver.user.dto.UserUpgradeCreateRequest;
import com.novaserver.user.dto.UserUpgradeResponse;
import com.novaserver.user.service.UserService;
import com.novaserver.user.service.UserStageProgressService;
import com.novaserver.user.service.UserUpgradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 유저 상태 조회 API. */
@Tag(name = "유저", description = "유저 상태 조회 API")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserUpgradeService userUpgradeService;
    private final UserStageProgressService userStageProgressService;

    @Operation(summary = "내 정보 조회", description = "토큰의 유저 id로 유저 상태 정보를 조회합니다.")
    @GetMapping("/me")
    public UserDataResponse getMe(@AuthenticationPrincipal Long userId) {
        return userService.getMe(userId);
    }

    @Operation(
            summary = "강화 레벨업",
            description =
                    "저장된 누적 경험치로 해금을 검사하고 골드·재료를 차감합니다. 현재 강화 레벨 L에서 "
                            + "levelCosts[L]·materialCosts[L] 우선, 각 배열이 null일 때만 기본값+증가값*L. "
                            + "해금 전·최대 레벨·잔액/재료 부족은 409. 같은 createId는 최초 결과를 반환합니다.")
    @PostMapping("/upgrades")
    public UserUpgradeResponse levelUpUpgrade(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserUpgradeCreateRequest request) {
        return userUpgradeService.levelUp(userId, request);
    }

    @Operation(
            summary = "별등급 보상 수령",
            description = "아직 받지 못한 별등급 최초 보상을 수령합니다. 받을 보상이 없으면 409를 반환합니다.")
    @PostMapping("/stage-progress/claim")
    public UserStageProgressResponse claimStageRatingReward(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StageRatingRewardClaimRequest request) {
        return userStageProgressService.claimRatingReward(userId, request.getStageId());
    }
}
