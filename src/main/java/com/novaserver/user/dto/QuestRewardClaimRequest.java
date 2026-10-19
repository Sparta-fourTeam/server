package com.novaserver.user.dto;

import com.novaserver.quest.entity.QuestPeriod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 퀘스트 보상 수령 요청. */
@Getter
public class QuestRewardClaimRequest {
    @NotNull(message = "period는 필수값입니다.")
    private QuestPeriod period;
}
