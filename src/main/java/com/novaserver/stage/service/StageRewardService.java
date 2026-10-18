package com.novaserver.stage.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.stage.dto.ClearRewardResult;
import com.novaserver.stage.dto.StageDataResponse;
import com.novaserver.stage.dto.StageRewardRequest;
import com.novaserver.stage.dto.StageRewardResponse;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.entity.StageReward;
import com.novaserver.stage.entity.StageRewardItem;
import com.novaserver.stage.repository.StageRewardRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 스테이지 보상 관련 API. */
@Service
@RequiredArgsConstructor
public class StageRewardService {
    private final StageRewardRepository stageRewardRepository;
    private final StageService stageService;
    private final ItemRepository itemRepository;
    private final StageRewardItemValidator itemValidator;
    private final RewardRulesService rulesService;

    /** 스테이지 보상 목록을 조회한다. */
    @Transactional(readOnly = true)
    public List<StageRewardResponse> getStageRewardList() {
        return stageRewardRepository.findAll().stream().map(StageRewardResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StageRewardResponse getStageRewardDetail(Integer stageId) {
        return StageRewardResponse.from(findStageReward(stageId));
    }

    /** 유니티 클라이언트용 스테이지 보상 데이터를 전체 조회한다. */
    @Transactional(readOnly = true)
    public List<StageDataResponse> getStageRewardData() {
        return stageRewardRepository.findAll().stream().map(StageDataResponse::from).toList();
    }

    /** 스테이지 보상을 등록한다. 없는 stageId면 404, 이미 보상이 등록되어 있으면 409를 던진다. */
    @Transactional
    public StageRewardResponse createStageReward(Integer stageId, StageRewardRequest request) {
        rulesService.lockRules();
        final Stage stage = stageService.findStage(stageId);
        if (stageRewardRepository.existsById(stageId)) {
            throw ErrorCode.STAGE_REWARD_DUPLICATED.exception(stageId);
        }
        validateBalance(request);
        List<StageRewardItem> items = itemValidator.validate(request.getItems());
        try {
            StageReward saved =
                    stageRewardRepository.save(
                            new StageReward(
                                    stage,
                                    request.getBaseExp(),
                                    request.getThreeStarBonusExp(),
                                    request.getFinalCoin(),
                                    items));
            return StageRewardResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.STAGE_REWARD_DUPLICATED.exception(stageId);
        }
    }

    /** 스테이지 보상을 수정한다. 없는 stageId면 404를 던진다. */
    @Transactional
    public StageRewardResponse updateStageReward(Integer stageId, StageRewardRequest request) {
        rulesService.lockRules();
        StageReward reward = findStageReward(stageId);
        validateBalance(request);
        List<StageRewardItem> items = itemValidator.validate(request.getItems());
        reward.update(
                request.getBaseExp(),
                request.getThreeStarBonusExp(),
                request.getFinalCoin(),
                items);
        return StageRewardResponse.from(reward);
    }

    @Transactional
    public void deleteStageReward(Integer stageId) {
        stageRewardRepository.delete(findStageReward(stageId));
    }

    /** 스테이지 보상을 stageId로 찾는다. 없으면 404를 던진다. */
    public StageReward findStageReward(Integer stageId) {
        return stageRewardRepository
                .findById(stageId)
                .orElseThrow(() -> ErrorCode.STAGE_REWARD_NOT_FOUND.exception(stageId));
    }

    /** 완료 웨이브의 기본 보상. 별이 있으면 최종 웨이브까지 완료한 결과여야 한다. */
    public ClearRewardResult calculateClearReward(Integer stageId, int clearedWave, int rating) {
        if (clearedWave < 0
                || clearedWave > StageRewardSchedule.WAVE_COUNT
                || rating < 0
                || rating > 3
                || (rating > 0 && clearedWave != StageRewardSchedule.WAVE_COUNT)) {
            throw ErrorCode.BATTLE_RESULT_INVALID.exception();
        }
        var rules = rulesService.getRules();
        StageReward reward = findStageReward(stageId);
        var coinSchedule = rules.schedules().get("Coin");
        if (reward.getBaseExp() < 0
                || reward.getThreeStarBonusExp() < 0
                || reward.getFinalCoin() < coinSchedule.minimumFinal()) {
            throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
        }
        try {
            int bonusExp = rating == 3 ? reward.getThreeStarBonusExp() : 0;
            int exp = Math.addExact(clearedWave >= 1 ? reward.getBaseExp() : 0, bonusExp);
            int gold = coinSchedule.amount(clearedWave, reward.getFinalCoin());
            List<String> itemIds = reward.getItems().stream().map(StageRewardItem::itemId).toList();
            Map<String, Item> catalogItems =
                    itemRepository.findAllById(itemIds).stream()
                            .collect(Collectors.toMap(Item::getItemId, item -> item));
            Map<String, Integer> items = new HashMap<>();
            var seen = new java.util.HashSet<String>();
            for (StageRewardItem item : reward.getItems()) {
                if (!catalogItems.containsKey(item.itemId())) {
                    throw ErrorCode.ITEM_NOT_FOUND.exception(item.itemId());
                }
                StageRewardSchedule schedule =
                        rules.fixedItemSchedule(item.itemId())
                                .orElseThrow(ErrorCode.BATTLE_REWARD_UNAVAILABLE::exception);
                if (item.finalAmount() < schedule.minimumFinal() || !seen.add(item.itemId())) {
                    throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
                }
                int amount = schedule.amount(clearedWave, item.finalAmount());
                if (amount > 0) {
                    items.put(item.itemId(), amount);
                }
            }
            return new ClearRewardResult(exp, gold, items, bonusExp);
        } catch (ArithmeticException error) {
            throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
        }
    }

    private void validateBalance(StageRewardRequest request) {
        if (request.getBaseExp() < 0
                || request.getThreeStarBonusExp() < 0
                || (long) request.getBaseExp() + request.getThreeStarBonusExp() > Integer.MAX_VALUE
                || request.getFinalCoin()
                        < rulesService.getRules().schedules().get("Coin").minimumFinal()) {
            throw ErrorCode.STAGE_REWARD_BALANCE_INVALID.exception();
        }
    }
}
