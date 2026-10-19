package com.novaserver.quest.dto;

import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

/** 퀘스트 등록·수정 요청. */
@Getter
public class QuestRequest {
    @NotNull(message = "period는 필수값입니다.")
    private QuestPeriod period;

    @NotNull(message = "kind는 필수값입니다.")
    private QuestKind kind;

    @NotNull(message = "target은 필수값입니다.")
    @Positive(message = "target은 1 이상입니다.")
    private Integer target;

    @NotNull(message = "points는 필수값입니다.")
    @Positive(message = "points는 1 이상입니다.")
    private Integer points;
}
