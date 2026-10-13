package com.novaserver.cards.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 카드 변형 등록·수정 요청. */
@Getter
public class CardVariantRequest {
    @NotNull(message = "minPermanentLevel은 필수값입니다.")
    @Min(value = 0, message = "minPermanentLevel은 0 이상입니다.")
    private Integer minPermanentLevel;

    @NotBlank(message = "이름은 필수값입니다.")
    private String name;

    private String desc;
}
