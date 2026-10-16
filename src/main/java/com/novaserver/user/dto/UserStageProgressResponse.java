package com.novaserver.user.dto;

import com.novaserver.user.entity.UserStageProgress;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 유저 스테이지 진행도 응답. */
@Getter
@RequiredArgsConstructor
@Schema(description = "유저 스테이지 응답")
public class UserStageProgressResponse {
    @Schema(description = "스테이지 id")
    private final Integer stageId;

    @Schema(description = "클리어 등급")
    private final int clearRating;

    @Schema(description = "보상 수령 등급")
    private final int claimedRating;

    /** 스테이지 진행도 엔티티를 응답으로 변환한다. */
    public static UserStageProgressResponse from(UserStageProgress userStageProgress) {
        return new UserStageProgressResponse(
                userStageProgress.getStage().getStageId(),
                userStageProgress.getClearRating(),
                userStageProgress.getClaimedRating());
    }
}
