package com.novaserver.cards.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 필요 스킬 조건 등록 요청. */
@Getter
public class RequiredSkillRequest {
    @NotNull(message = "skillId는 필수값입니다.")
    private Long skillId;
}
