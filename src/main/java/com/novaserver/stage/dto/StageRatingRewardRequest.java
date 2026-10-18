package com.novaserver.stage.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

/** 별등급 보상 등록·수정 요청. */
@Getter
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public class StageRatingRewardRequest {
    @NotNull
    @Positive
    @Schema(description = "연결할 공통 고정 보상 묶음 ID. 필수 양수이며 묶음은 별도로 등록합니다.")
    private Long rewardDefinitionId;

    /** 계약에 없는 입력 필드를 무시하지 않고 역직렬화 오류로 거절한다. */
    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("지원하지 않는 별등급 보상 입력 필드입니다: " + name);
    }
}
