package com.novaserver.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** 단일 퀘스트 완료 보상 수령 요청. */
@Getter
public class QuestClaimRequest {
    @NotBlank(message = "questId는 필수값입니다.")
    private String questId;
}
