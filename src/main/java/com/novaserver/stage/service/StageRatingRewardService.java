package com.novaserver.stage.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.stage.dto.StageRatingRewardRequest;
import com.novaserver.stage.dto.StageRatingRewardResponse;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.entity.StageRatingReward;
import com.novaserver.stage.repository.StageRatingRewardRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 별등급 최초 클리어 보상 관련 API. */
@Service
@RequiredArgsConstructor
public class StageRatingRewardService {
    private final StageRatingRewardRepository stageRatingRewardRepository;
    private final StageService stageService;
    private final ItemRepository items;

    /** 스테이지의 별등급 보상 목록을 조회한다. */
    @Transactional(readOnly = true)
    public List<StageRatingRewardResponse> getStageRatingRewardList(Integer stageId) {
        return stageRatingRewardRepository.findByStageStageIdOrderByRating(stageId).stream()
                .map(StageRatingRewardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StageRatingRewardResponse getStageRatingReward(Integer stageId, int rating) {
        return StageRatingRewardResponse.from(findStageRatingReward(stageId, rating));
    }

    /** 별등급 보상을 등록한다. 없는 stageId면 404, 이미 등록되어 있으면 409를 던진다. */
    @Transactional
    public StageRatingRewardResponse createStageRatingReward(
            Integer stageId, int rating, StageRatingRewardRequest request) {
        Stage stage = stageService.findStage(stageId);
        if (stageRatingRewardRepository.existsByStageStageIdAndRating(stageId, rating)) {
            throw ErrorCode.STAGE_RATING_REWARD_DUPLICATED.exception(stageId, rating);
        }

        try {
            StageRatingReward saved =
                    stageRatingRewardRepository.save(
                            new StageRatingReward(
                                    stage, rating, request.getGold(), validateItems(request)));
            return StageRatingRewardResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.STAGE_RATING_REWARD_DUPLICATED.exception(stageId, rating);
        }
    }

    /** 별등급 보상을 수정한다. 없으면 404를 던진다. */
    @Transactional
    public StageRatingRewardResponse updateStageRatingReward(
            Integer stageId, int rating, StageRatingRewardRequest request) {
        StageRatingReward reward = findStageRatingReward(stageId, rating);
        reward.update(request.getGold(), validateItems(request));
        return StageRatingRewardResponse.from(reward);
    }

    @Transactional
    public void deleteStageRatingReward(Integer stageId, int rating) {
        stageRatingRewardRepository.delete(findStageRatingReward(stageId, rating));
    }

    private Map<String, Integer> validateItems(StageRatingRewardRequest request) {
        var requested = request.getItems() == null ? Map.<String, Integer>of() : request.getItems();
        if (request.getGold() == null
                || request.getGold() < 0
                || requested.entrySet().stream()
                        .anyMatch(
                                entry ->
                                        entry.getKey() == null
                                                || entry.getKey().isBlank()
                                                || entry.getValue() == null
                                                || entry.getValue() <= 0)) {
            throw ErrorCode.STAGE_RATING_REWARD_INVALID.exception();
        }
        var catalog =
                items.findAllById(requested.keySet()).stream()
                        .map(Item::getItemId)
                        .collect(Collectors.toSet());
        for (String id : requested.keySet()) {
            if (!catalog.contains(id)) {
                throw ErrorCode.ITEM_NOT_FOUND.exception(id);
            }
        }
        return Map.copyOf(requested);
    }

    private StageRatingReward findStageRatingReward(Integer stageId, int rating) {
        return stageRatingRewardRepository
                .findByStageStageIdAndRating(stageId, rating)
                .orElseThrow(
                        () -> ErrorCode.STAGE_RATING_REWARD_NOT_FOUND.exception(stageId, rating));
    }
}
