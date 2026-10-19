package com.novaserver.quest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/** 퀘스트 등록 요청. */
@Getter
public class QuestCreateRequest extends QuestRequest {
    @NotBlank(message = "퀘스트 id는 필수값입니다.")
    @Size(max = 255, message = "퀘스트 id는 255자 이하입니다.")
    private String questId;
}
