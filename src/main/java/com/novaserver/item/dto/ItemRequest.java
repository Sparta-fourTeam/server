package com.novaserver.item.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

/** 아이템 수정 요청. */
@Getter
public class ItemRequest {
    @Size(max = 255, message = "targetId는 255자 이하입니다.")
    private String targetId;
}
