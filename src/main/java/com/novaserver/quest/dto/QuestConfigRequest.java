package com.novaserver.quest.dto;

import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 클라 진행 입력이 아닌 관리자 전용 퀘스트 조건. */
public record QuestConfigRequest(
        @NotNull QuestPeriod period,
        @NotNull QuestKind kind,
        @Min(1) int target,
        @Min(1) int points) {}
