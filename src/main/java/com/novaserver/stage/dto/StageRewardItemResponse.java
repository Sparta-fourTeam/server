package com.novaserver.stage.dto;

import com.novaserver.stage.entity.StageRewardItem;

/** 스테이지 보상 아이템 응답. */
public record StageRewardItemResponse(String itemId, int finalAmount) {

    /** JSON 엔트리로부터 응답을 만든다. */
    public static StageRewardItemResponse from(StageRewardItem item) {
        return new StageRewardItemResponse(item.itemId(), item.finalAmount());
    }
}
