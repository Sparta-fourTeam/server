package com.novaserver.tutorial.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

/** 완료할 튜토리얼 ID만 받는다. 지급액과 조건 충족 여부는 서버가 결정한다. */
@Getter
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public class TutorialCompleteRequest {
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{1,64}(\\.dialogue)?")
    private String tutorialId;

    /** 클라이언트가 보상이나 진행 조건을 지정한 입력은 거절한다. */
    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("지원하지 않는 튜토리얼 입력 필드입니다: " + name);
    }
}
