package com.novaserver.stage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;
import lombok.Getter;

/** 별등급 보상 등록·수정 요청. */
@Getter
public class StageRatingRewardRequest {
    @NotNull(message = "gold는 필수값입니다.")
    @PositiveOrZero(message = "gold는 0 이상입니다.")
    private Integer gold;

    @Schema(description = "실제 카탈로그 itemId별 지급 수량. 생략/null은 아이템 없음, 추첨은 수행하지 않음")
    private Map<@NotBlank String, @NotNull @Positive Integer> items;
}
