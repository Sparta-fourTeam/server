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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 스테이지 보상 관련 API. */
@Service
@RequiredArgsConstructor
public class StageRewardService {
    /** 배틀 종료 시 기본 보상 계산에 쓰이는 고정값. 모든 스테이지에 동일하게 적용된다. */
    private static final int TOTAL_WAVES = 20;

    private static final int COIN_START = 25;
    private static final int COIN_INCREMENT = 5;
    private static final int COIN_BONUS_WAVE = 6;
    private static final int MATERIAL_START = 1;
    private static final int MATERIAL_INCREMENT = 1;
    private static final int SKILL_MATERIAL_BONUS_WAVE = 3;
    private static final int EQUIPMENT_MATERIAL_WAVE = 4;
    private static final int THREE_STAR_RATING = 3;

    private final StageRewardRepository stageRewardRepository;
    private final StageService stageService;
    private final ItemRepository itemRepository;

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
        Stage stage = stageService.findStage(stageId);
        if (stageRewardRepository.existsById(stageId)) {
            throw ErrorCode.STAGE_REWARD_DUPLICATED.exception(stageId);
        }
        List<StageRewardItem> items = StageRewardItemValidator.validate(request.getItems());
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
        StageReward reward = findStageReward(stageId);
        List<StageRewardItem> items = StageRewardItemValidator.validate(request.getItems());
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

    /**
     * 배틀 종료 시 즉시 지급할 기본 보상을 계산한다. clearedWave가 0이면 보상이 없다.
     *
     * @param stageId 스테이지 id
     * @param clearedWave 이번 배틀에서 클리어한 웨이브
     * @param rating 이번 배틀의 클리어 등급(0~3)
     */
    public ClearRewardResult calculateClearReward(Integer stageId, int clearedWave, int rating) {
        StageReward reward = findStageReward(stageId);
        if (clearedWave <= 0) {
            return new ClearRewardResult(0, 0, Map.of());
        }

        int exp = reward.getBaseExp() * clearedWave;
        if (rating == THREE_STAR_RATING) {
            exp += reward.getThreeStarBonusExp();
        }

        int gold = COIN_START + COIN_INCREMENT * (clearedWave - 1);
        if (clearedWave >= COIN_BONUS_WAVE) {
            gold += reward.getFinalCoin() - COIN_START - COIN_INCREMENT * TOTAL_WAVES;
        }

        Map<String, Integer> items = new HashMap<>();
        for (StageRewardItem item : reward.getItems()) {
            int amount = calculateItemAmount(item, clearedWave);
            if (amount > 0) {
                items.put(item.itemId(), amount);
            }
        }

        return new ClearRewardResult(exp, gold, items);
    }

    /** 스킬 재료(Item.targetId 있음)는 웨이브마다 누적 증가, 장비 재료는 지정 웨이브 클리어 시 전량 지급된다. */
    private int calculateItemAmount(StageRewardItem item, int clearedWave) {
        Item catalogItem =
                itemRepository
                        .findById(item.itemId())
                        .orElseThrow(() -> ErrorCode.ITEM_NOT_FOUND.exception(item.itemId()));

        if (catalogItem.getTargetId() != null) {
            int amount = MATERIAL_START + MATERIAL_INCREMENT * (clearedWave - 1);
            if (clearedWave >= SKILL_MATERIAL_BONUS_WAVE) {
                amount += item.finalAmount() - MATERIAL_START - MATERIAL_INCREMENT * TOTAL_WAVES;
            }
            return amount;
        }

        return clearedWave >= EQUIPMENT_MATERIAL_WAVE ? item.finalAmount() : 0;
    }
}
