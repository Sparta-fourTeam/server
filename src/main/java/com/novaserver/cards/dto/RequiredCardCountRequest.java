package com.novaserver.cards.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 선행 카드 조건 등록 요청. */
@Getter
public class RequiredCardCountRequest {
    @NotNull(message = "requiredCardId는 필수값입니다.")
    private Long requiredCardId;

    @Min(value = 1, message = "count는 1 이상입니다.")
    private Integer count;
}
