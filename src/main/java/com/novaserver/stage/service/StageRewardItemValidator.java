package com.novaserver.stage.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.stage.dto.StageRewardItemRequest;
import com.novaserver.stage.entity.StageRewardItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 스테이지 보상·별등급 보상이 공유하는 아이템 목록 검증. */
@Component
@RequiredArgsConstructor
class StageRewardItemValidator {
    private static final int SKILL_MATERIAL_MIN_AMOUNT = 21;

    private final ItemRepository itemRepository;

    /**
     * 아이템이 존재하는지, 중복되지 않았는지 확인하고, 스킬 재료(Item.targetId 있음)는 finalAmount가 21 이상인지 확인한다. 스킬 재료·장비 재료
     * 판별 기준은 배틀 종료 시 보상 계산({@link StageRewardService#calculateClearReward})과 동일하게 Item.targetId 존재
     * 여부로 통일한다.
     */
    List<StageRewardItem> validate(List<StageRewardItemRequest> requests) {
        if (requests == null) {
            return new ArrayList<>();
        }
        List<StageRewardItem> items = new ArrayList<>();
        Set<String> itemIdsSeen = new HashSet<>();
        for (StageRewardItemRequest request : requests) {
            String itemId = request.getItemId();
            if (!itemIdsSeen.add(itemId)) {
                throw ErrorCode.STAGE_REWARD_ITEM_DUPLICATED.exception(itemId);
            }

            Item item =
                    itemRepository
                            .findById(itemId)
                            .orElseThrow(() -> ErrorCode.ITEM_NOT_FOUND.exception(itemId));
            if (item.getTargetId() != null
                    && request.getFinalAmount() < SKILL_MATERIAL_MIN_AMOUNT) {
                throw ErrorCode.STAGE_REWARD_ITEM_AMOUNT_INVALID.exception(
                        request.getFinalAmount());
            }

            items.add(new StageRewardItem(itemId, request.getFinalAmount()));
        }
        return items;
    }
}
