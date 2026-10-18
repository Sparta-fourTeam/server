package com.novaserver.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/** 아이템 수정 요청. */
@Getter
public class ItemRequest {
    @Size(max = 255, message = "targetId는 255자 이하입니다.")
    @Schema(description = "스킬 숫자 ID 또는 장비 강화 ID. 대상이 없는 상자는 null")
    private String targetId;
}
