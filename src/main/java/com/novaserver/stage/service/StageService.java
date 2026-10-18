package com.novaserver.stage.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.stage.dto.StageRequest;
import com.novaserver.stage.dto.StageResponse;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.repository.StageRatingRewardRepository;
import com.novaserver.stage.repository.StageRepository;
import com.novaserver.stage.repository.StageRewardRepository;
import com.novaserver.tutorial.repository.TutorialRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 스테이지 관련 API. */
@Service
@RequiredArgsConstructor
public class StageService {
    private final TutorialRepository tutorials;
    private final RewardRulesService referenceLocks;
    private final StageRepository stageRepository;
    private final StageRewardRepository stageRewardRepository;
    private final StageRatingRewardRepository stageRatingRewardRepository;

    /** 스테이지 목록을 조회한다. */
    @Transactional(readOnly = true)
    public List<StageResponse> getStageList() {
        return stageRepository.findAll().stream().map(StageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StageResponse getStage(Integer stageId) {
        return StageResponse.from(findStage(stageId));
    }

    /** 스테이지를 등록한다. stageId가 이미 있으면 409를 던진다. */
    @Transactional
    public StageResponse createStage(StageRequest request) {
        if (stageRepository.existsById(request.getStageId())) {
            throw ErrorCode.STAGE_DUPLICATED.exception(request.getStageId());
        }
        try {
            Stage saved = stageRepository.save(Stage.create(request.getStageId()));
            return StageResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.STAGE_DUPLICATED.exception(request.getStageId());
        }
    }

    /** 스테이지를 삭제한다. 없는 stageId면 404, 등록된 보상(기본 보상·별등급 보상)이 있으면 409를 던진다. */
    @Transactional
    public void deleteStage(Integer stageId) {
        referenceLocks.lockItemReferences();
        final Stage stage = findStage(stageId);
        if (tutorials.existsByStartStageStageIdOrClearStageStageId(stageId, stageId)) {
            throw ErrorCode.TUTORIAL_IN_USE.exception();
        }
        if (stageRewardRepository.existsById(stageId)) {
            throw ErrorCode.STAGE_REWARD_EXISTS.exception(stageId);
        }
        if (stageRatingRewardRepository.existsByStageStageId(stageId)) {
            throw ErrorCode.STAGE_REWARD_EXISTS.exception(stageId);
        }
        stageRepository.delete(stage);
    }

    /** 스테이지를 stageId로 찾는다. 없으면 404를 던진다. */
    public Stage findStage(Integer stageId) {
        return stageRepository
                .findById(stageId)
                .orElseThrow(() -> ErrorCode.STAGE_NOT_FOUND.exception(stageId));
    }
}
