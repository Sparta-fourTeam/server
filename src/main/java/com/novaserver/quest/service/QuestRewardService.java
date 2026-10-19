package com.novaserver.quest.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.quest.dto.QuestRewardCreateRequest;
import com.novaserver.quest.dto.QuestRewardRequest;
import com.novaserver.quest.dto.QuestRewardResponse;
import com.novaserver.quest.entity.QuestReward;
import com.novaserver.quest.repository.QuestRewardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 퀘스트 보상 정의 관련 API. */
@Service
@RequiredArgsConstructor
public class QuestRewardService {
    private final QuestRewardRepository questRewardRepository;

    /** 보상을 전체 조회한다. */
    @Transactional(readOnly = true)
    public List<QuestRewardResponse> getQuestRewardList() {
        return questRewardRepository.findAll(Sort.by("rewardId")).stream()
                .map(QuestRewardResponse::from)
                .toList();
    }

    /** 보상 한 건을 조회한다. 없으면 404를 던진다. */
    @Transactional(readOnly = true)
    public QuestRewardResponse getQuestReward(String rewardId) {
        return QuestRewardResponse.from(findQuestReward(rewardId));
    }

    /** 보상을 등록한다. 이미 있는 id면 409를 던진다. */
    @Transactional
    public QuestRewardResponse createQuestReward(QuestRewardCreateRequest request) {
        if (questRewardRepository.existsById(request.getRewardId())) {
            throw ErrorCode.QUEST_REWARD_DUPLICATED.exception(request.getRewardId());
        }
        if (questRewardRepository.existsByPeriodAndPoints(
                request.getPeriod(), request.getPoints())) {
            throw ErrorCode.QUEST_REWARD_THRESHOLD_DUPLICATED.exception(
                    request.getPeriod(), request.getPoints());
        }

        try {
            QuestReward saved =
                    questRewardRepository.saveAndFlush(
                            new QuestReward(
                                    request.getRewardId(),
                                    request.getPeriod(),
                                    request.getPoints(),
                                    request.getGold(),
                                    request.getEnergy()));
            return QuestRewardResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.QUEST_REWARD_DUPLICATED.exception(request.getRewardId());
        }
    }

    /** 보상을 수정한다. 없는 id면 404를 던진다. */
    @Transactional
    public QuestRewardResponse updateQuestReward(String rewardId, QuestRewardRequest request) {
        QuestReward reward = findQuestReward(rewardId);
        reward.update(request.getGold(), request.getEnergy());
        return QuestRewardResponse.from(reward);
    }

    /** 보상을 삭제한다. 없는 id면 404를 던진다. */
    @Transactional
    public void deleteQuestReward(String rewardId) {
        questRewardRepository.delete(findQuestReward(rewardId));
    }

    private QuestReward findQuestReward(String rewardId) {
        return questRewardRepository
                .findById(rewardId)
                .orElseThrow(() -> ErrorCode.QUEST_REWARD_NOT_FOUND.exception(rewardId));
    }
}
