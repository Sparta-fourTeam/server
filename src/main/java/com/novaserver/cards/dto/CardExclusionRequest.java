package com.novaserver.cards.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 카드 배제 조건 등록 요청. */
@Getter
public class CardExclusionRequest {
    @NotNull(message = "excludingCardId는 필수값입니다.")
    private Long excludingCardId;

    @Min(value = 0, message = "belowPermanentLevel은 0 이상입니다.")
    private Integer belowPermanentLevel;
}
