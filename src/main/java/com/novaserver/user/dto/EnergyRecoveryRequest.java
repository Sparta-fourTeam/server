package com.novaserver.user.dto;

import com.novaserver.user.EnergyRecoverySource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 추가 에너지 회복 요청. 지급량과 가격은 서버에서 결정한다. */
public record EnergyRecoveryRequest(
        @NotBlank @Size(max = 128) @Schema(description = "계정 내 회복 요청의 멱등 키. 날짜가 바뀌어도 재사용 금지")
                String createId,
        @NotNull @Schema(description = "Purchase 또는 Ad. Ad는 검증 미연결로 503")
                EnergyRecoverySource source) {}
