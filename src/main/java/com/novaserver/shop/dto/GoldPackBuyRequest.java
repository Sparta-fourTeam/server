package com.novaserver.shop.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

/** 조회한 카탈로그 버전·팩 순서 번호와 요청 키만 받는다. 금액·가격 주장은 거절한다. */
@Getter
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public class GoldPackBuyRequest {
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{1,64}")
    private String requestId;

    @NotNull
    @Min(1)
    private Integer configVersion;

    @NotNull
    @Min(0)
    private Integer index;

    /** 클라이언트의 가격·지급량·사용자 ID 등 추가 입력을 수신하지 않는다. */
    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("지원하지 않는 골드팩 입력 필드입니다: " + name);
    }
}
