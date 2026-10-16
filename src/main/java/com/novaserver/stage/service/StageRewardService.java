package com.novaserver.stage.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.stage.dto.StageDataResponse;
import com.novaserver.stage.dto.StageRewardItemRequest;
import com.novaserver.stage.dto.StageRewardRequest;
import com.novaserver.stage.dto.StageRewardResponse;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.entity.StageReward;
import com.novaserver.stage.entity.StageRewardItem;
import com.novaserver.stage.repository.StageRewardRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 스테이지 보상 관련 API. */
@Service
@RequiredArgsConstructor
public class StageRewardService {
    private static final String SKILL_MATERIAL_PREFIX = "material.skill";
    private static final int SKILL_MATERIAL_MIN_AMOUNT = 21;

    private final StageRewardRepository stageRewardRepository;
    private final StageService stageService;

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
        List<StageRewardItem> items = validatedItems(request.getItems());
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
        List<StageRewardItem> items = validatedItems(request.getItems());
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

    /** 아이템 중복을 막고, 스킬 재료는 finalAmount가 21 이상인지 확인한다. */
    private List<StageRewardItem> validatedItems(List<StageRewardItemRequest> requests) {
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
