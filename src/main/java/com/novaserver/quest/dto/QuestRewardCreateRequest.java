package com.novaserver.quest.dto;

import com.novaserver.quest.entity.QuestPeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/** 퀘스트 보상 등록 요청. */
@Getter
public class QuestRewardCreateRequest {
    @NotBlank(message = "보상 id는 필수값입니다.")
    @Size(max = 255, message = "보상 id는 255자 이하입니다.")
    private String rewardId;

    @NotNull(message = "period는 필수값입니다.")
    private QuestPeriod period;

    @NotNull(message = "points는 필수값입니다.")
    @Positive(message = "points는 1 이상입니다.")
    private Integer points;

    @NotNull(message = "gold는 필수값입니다.")
    @PositiveOrZero(message = "gold는 0 이상입니다.")
    private Integer gold;

    @NotNull(message = "energy는 필수값입니다.")
    @PositiveOrZero(message = "energy는 0 이상입니다.")
    private Integer energy;
}
