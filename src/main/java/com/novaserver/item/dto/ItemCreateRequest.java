package com.novaserver.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/** 아이템 등록 요청. */
@Getter
public class ItemCreateRequest extends ItemRequest {
    @NotBlank(message = "아이템 id는 필수값입니다.")
    @Size(max = 255, message = "아이템 id는 255자 이하입니다.")
    private String itemId;
}
