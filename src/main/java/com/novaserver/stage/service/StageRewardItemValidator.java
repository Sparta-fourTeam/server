package com.novaserver.stage.service;

import com.novaserver.global.error.ErrorCode;
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
    private final ItemRepository itemRepository;
    private final RewardRulesService rulesService;

    /** 카탈로그 존재·중복·지원하는 지급 종류와 도달 가능한 최종 수량을 확인한다. */
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

            itemRepository
                    .findById(itemId)
                    .orElseThrow(() -> ErrorCode.ITEM_NOT_FOUND.exception(itemId));
            StageRewardSchedule schedule =
                    rulesService
                            .getRules()
                            .fixedItemSchedule(itemId)
                            .orElseThrow(
                                    () ->
                                            ErrorCode.STAGE_REWARD_ITEM_UNSUPPORTED.exception(
                                                    itemId));
            if (request.getFinalAmount() < schedule.minimumFinal()) {
                throw ErrorCode.STAGE_REWARD_ITEM_AMOUNT_INVALID.exception(
                        request.getFinalAmount());
            }

            items.add(new StageRewardItem(itemId, request.getFinalAmount()));
        }
        return items;
    }
}
