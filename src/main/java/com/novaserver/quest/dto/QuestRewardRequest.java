package com.novaserver.quest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;

/** 퀘스트 보상 수정 요청. period·points(포인트 구간)는 등록 후 바꿀 수 없어 수정 대상이 아니다. */
@Getter
public class QuestRewardRequest {
    @NotNull(message = "gold는 필수값입니다.")
    @PositiveOrZero(message = "gold는 0 이상입니다.")
    private Integer gold;

    @NotNull(message = "energy는 필수값입니다.")
    @PositiveOrZero(message = "energy는 0 이상입니다.")
    private Integer energy;
}
