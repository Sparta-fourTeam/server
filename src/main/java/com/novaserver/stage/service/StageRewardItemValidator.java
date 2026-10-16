package com.novaserver.stage.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.stage.dto.StageRewardItemRequest;
import com.novaserver.stage.entity.StageRewardItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 스테이지 보상·별등급 보상이 공유하는 아이템 목록 검증. */
final class StageRewardItemValidator {
    private static final String SKILL_MATERIAL_PREFIX = "material.skill";
    private static final int SKILL_MATERIAL_MIN_AMOUNT = 21;

    private StageRewardItemValidator() {}

    /** 아이템 중복을 막고, 스킬 재료는 finalAmount가 21 이상인지 확인한다. */
    static List<StageRewardItem> validate(List<StageRewardItemRequest> requests) {
        if (requests == null) {
            return new ArrayList<>();
        }
        List<StageRewardItem> items = new ArrayList<>();
        Set<String> itemIdsSeen = new HashSet<>();
        for (StageRewardItemRequest request : requests) {
            if (!itemIdsSeen.add(request.getItemId())) {
                throw ErrorCode.STAGE_REWARD_ITEM_DUPLICATED.exception(request.getItemId());
            }
            if (request.getItemId().startsWith(SKILL_MATERIAL_PREFIX)
                    && request.getFinalAmount() < SKILL_MATERIAL_MIN_AMOUNT) {
                throw ErrorCode.STAGE_REWARD_ITEM_AMOUNT_INVALID.exception(
                        request.getFinalAmount());
            }
            items.add(new StageRewardItem(request.getItemId(), request.getFinalAmount()));
        }
        return items;
    }
}
