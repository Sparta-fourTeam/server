package com.novaserver.item.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novaserver.item.entity.Item;

/** 아이템 데이터 응답. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemResponse(String itemId, String targetId) {

    /** 아이템 엔티티로 응답을 만든다. */
    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getItemId(), item.getTargetId());
    }
}
