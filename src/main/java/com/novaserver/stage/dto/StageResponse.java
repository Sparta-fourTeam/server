package com.novaserver.stage.dto;

import com.novaserver.stage.entity.Stage;

/** 웹용 스테이지 응답. */
public record StageResponse(Integer stageId) {

    /** 스테이지 엔티티로부터 응답을 만든다. */
    public static StageResponse from(Stage stage) {
        return new StageResponse(stage.getStageId());
    }
}
