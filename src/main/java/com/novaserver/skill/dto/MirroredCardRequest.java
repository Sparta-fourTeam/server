package com.novaserver.skill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 카드 효과 적용 조건 등록 요청. */
@Getter
public class MirroredCardRequest {
    @NotNull(message = "cardId는 필수값입니다.")
    private Long cardId;
}
