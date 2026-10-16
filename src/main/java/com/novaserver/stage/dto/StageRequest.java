package com.novaserver.stage.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** 스테이지 등록 요청. */
@Getter
public class StageRequest {
    @NotNull(message = "stageId는 필수값입니다.")
    private Integer stageId;
}
