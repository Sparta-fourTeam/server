package com.novaserver.shop.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

/** 조회한 서버 날짜·회차와 요청 키만 받으며 임의 가격·보상은 거절한다. */
@Getter
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public class ShopBuyRequest {
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{1,64}")
    private String requestId;

    @NotBlank
    @Pattern(regexp = "[0-9]{8}")
    private String dayKey;

    @NotNull
    @Min(0)
    private Integer rerollIndex;

    @NotNull
    @Min(0)
    private Integer slot;

    /** 서버가 정할 재화·보상 필드 등은 수신하지 않는다. */
    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("지원하지 않는 상점 입력 필드입니다: " + name);
    }
}
