package com.novaserver.cards.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 강화 카드 등록·수정 요청. */
@Getter
public class CardRequest {
    @NotNull(message = "소속 스킬은 필수값입니다.")
    private Long skillId;

    @NotBlank(message = "이름은 필수값입니다.")
    private String name;

    private String desc;

    @Min(value = 1, message = "최소 값은 1 이상입니다.")
    private Integer maxPickCount;

    private Boolean enabled;

    private String disabledReason;

    @Min(value = 1, message = "최소 값은 1 이상입니다.")
    private Integer minBattleLevel;

    @Min(value = 0, message = "최소 값은 0 이상입니다.")
    private Integer minPermanentLevel;

    private String sharedId;
}
