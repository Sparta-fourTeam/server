package com.novaserver.quest.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.novaserver.quest.entity.QuestPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

/** 화면에서 조회한 서버 기간키와 정의 ID만 받는다. 진행도·보상 주장은 거절한다. */
@Getter
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public class QuestClaimRequest {
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_.-]{1,64}")
    private String id;

    @NotNull private QuestPeriod period;

    @NotBlank
    @Pattern(regexp = "[0-9]{8}")
    private String periodKey;

    /** 임의 진행도와 보상 필드는 수신하지 않는다. */
    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("지원하지 않는 퀘스트 입력 필드입니다: " + name);
    }
}
