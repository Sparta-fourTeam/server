package com.novaserver.quest.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.quest.dto.QuestCreateRequest;
import com.novaserver.quest.dto.QuestRequest;
import com.novaserver.quest.dto.QuestResponse;
import com.novaserver.quest.entity.Quest;
import com.novaserver.quest.repository.QuestRepository;
import com.novaserver.user.repository.UserQuestProgressRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 일일·주간 퀘스트 정의 관련 API. */
@Service
@RequiredArgsConstructor
public class QuestService {
    private final QuestRepository questRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;

    /** 퀘스트를 전체 조회한다. */
    @Transactional(readOnly = true)
    public List<QuestResponse> getQuestList() {
        return questRepository.findAll(Sort.by("questId")).stream()
                .map(QuestResponse::from)
                .toList();
    }

    /** 퀘스트 한 건을 조회한다. 없으면 404를 던진다. */
    @Transactional(readOnly = true)
    public QuestResponse getQuest(String questId) {
        return QuestResponse.from(findQuest(questId));
    }

    /** 퀘스트를 등록한다. 이미 있는 id면 409를 던진다. */
    @Transactional
    public QuestResponse createQuest(QuestCreateRequest request) {
        // id를 직접 지정하는 엔티티라 save()가 기존 행을 덮어쓰므로, 먼저 존재 여부를 확인한다.
        if (questRepository.existsById(request.getQuestId())) {
            throw ErrorCode.QUEST_DUPLICATED.exception(request.getQuestId());
        }

        try {
            Quest saved =
                    questRepository.saveAndFlush(
                            new Quest(
                                    request.getQuestId(),
                                    request.getPeriod(),
                                    request.getKind(),
                                    request.getTarget(),
                                    request.getPoints()));
            return QuestResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.QUEST_DUPLICATED.exception(request.getQuestId());
        }
    }

    /** 퀘스트를 수정한다. 없는 id면 404를 던진다. */
    @Transactional
    public QuestResponse updateQuest(String questId, QuestRequest request) {
        Quest quest = findQuest(questId);
        quest.update(request.getKind(), request.getTarget(), request.getPoints());
        return QuestResponse.from(quest);
    }

    /** 퀘스트를 삭제한다. 없는 id면 404, 진행 중인 유저가 있으면 409를 던진다. */
    @Transactional
    public void deleteQuest(String questId) {
        Quest quest = findQuest(questId);
        if (userQuestProgressRepository.existsByQuestQuestId(questId)) {
            throw ErrorCode.QUEST_IN_USE.exception(questId);
        }
        questRepository.delete(quest);
    }

    /** 퀘스트를 id로 찾는다. 없으면 404를 던진다. */
    public Quest findQuest(String questId) {
        return questRepository
                .findById(questId)
                .orElseThrow(() -> ErrorCode.QUEST_NOT_FOUND.exception(questId));
    }
}
