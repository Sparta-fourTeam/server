package com.novaserver.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/** 별등급 보상 수령 요청. */
@Getter
public class StageRatingRewardClaimRequest {
    @NotNull(message = "stageId는 필수값입니다.")
    private Integer stageId;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "계정 내 수령 요청 키. 재전송에는 같은 키, 새로 달성한 별 수령에는 새 키 사용")
    private String createId;

    @NotNull
    @Min(1)
    @Max(3)
    @Schema(description = "이번 요청에서 수령할 목표 별 등급. 재전송 때 변경 금지")
    private Integer targetRating;
}
