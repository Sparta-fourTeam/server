package com.novaserver.quest.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.quest.dto.QuestChestRequest;
import com.novaserver.quest.dto.QuestChestResponse;
import com.novaserver.quest.dto.QuestConfigRequest;
import com.novaserver.quest.dto.QuestConfigResponse;
import com.novaserver.quest.entity.QuestChest;
import com.novaserver.quest.entity.QuestDefinition;
import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.repository.QuestChestRepository;
import com.novaserver.quest.repository.QuestDefinitionRepository;
import com.novaserver.quest.repository.UserQuestClaimRepository;
import com.novaserver.reward.service.RewardDefinitionService;
import com.novaserver.stage.service.RewardRulesService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 검증 가능한 종류의 퀘스트·상자만 등록한다. 운영 정의와 밸런스를 자동 생성하지 않는다. */
@Service
@RequiredArgsConstructor
public class QuestConfigService {
    private final QuestDefinitionRepository quests;
    private final QuestChestRepository chests;
    private final UserQuestClaimRepository claims;
    private final RewardDefinitionService rewards;
    private final RewardRulesService locks;

    @Transactional(readOnly = true)
    public List<QuestConfigResponse> quests() {
        return quests.findAll().stream().map(QuestConfigResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<QuestChestResponse> chests() {
        return chests.findAll().stream().map(QuestChestResponse::from).toList();
    }

    /** 수령 이력이 있는 정의는 변경하지 않고 새 ID로 등록하게 한다. */
    @Transactional
    public QuestConfigResponse saveQuest(String id, QuestConfigRequest r) {
        locks.lockItemReferences();
        validateId(id);
        if (r == null
                || r.period() == null
                || r.kind() == null
                || r.target() < 1
                || r.points() < 1
                || (r.kind() == QuestKind.LOGIN
                        && r.target() > (r.period() == QuestPeriod.DAILY ? 1 : 7))
                || chests.existsById(id)) {
            throw ErrorCode.QUEST_INVALID.exception();
        }
        unused(id);
        return QuestConfigResponse.from(
                quests.saveAndFlush(
                        new QuestDefinition(id, r.period(), r.kind(), r.target(), r.points())));
    }

    /** 상자는 공통 고정 보상을 참조하며 에너지·젬 추가 지급량도 서버가 결정한다. */
    @Transactional
    public QuestChestResponse saveChest(String id, QuestChestRequest r) {
        locks.lockItemReferences();
        validateId(id);
        if (r == null
                || r.period() == null
                || r.points() < 1
                || r.rewardDefinitionId() == null
                || r.rewardDefinitionId() < 1
                || r.energy() == null
                || r.energy() < 0
                || r.gems() == null
                || r.gems() < 0
                || quests.existsById(id)) {
            throw ErrorCode.QUEST_INVALID.exception();
        }
        unused(id);
        var reward = rewards.findDefinition(r.rewardDefinitionId());
        if (reward.getGold() == 0
                && reward.itemAmounts().isEmpty()
                && r.energy() == 0
                && r.gems() == 0) {
            throw ErrorCode.QUEST_INVALID.exception();
        }
        return QuestChestResponse.from(
                chests.saveAndFlush(
                        new QuestChest(id, r.period(), r.points(), reward, r.energy(), r.gems())));
    }

    /** 수령 이력이 있으면 점수 의미와 영수증을 보존하기 위해 삭제를 거절한다. */
    @Transactional
    public void deleteQuest(String id) {
        locks.lockItemReferences();
        unused(id);
        var q = quests.findById(id).orElseThrow(() -> ErrorCode.QUEST_NOT_FOUND.exception(id));
        quests.delete(q);
        quests.flush();
    }

    /** 수령한 상자는 삭제하지 않고 새 ID의 상자를 사용한다. */
    @Transactional
    public void deleteChest(String id) {
        locks.lockItemReferences();
        unused(id);
        var c = chests.findById(id).orElseThrow(() -> ErrorCode.QUEST_NOT_FOUND.exception(id));
        chests.delete(c);
        chests.flush();
    }

    private void unused(String id) {
        if (claims.existsByDefinitionId(id)) {
            throw ErrorCode.QUEST_IN_USE.exception();
        }
    }

    private void validateId(String id) {
        if (id == null
                || !id.matches("[A-Za-z0-9_.-]{1,64}")
                || quests.findCanonicalId(id).filter(actual -> !actual.equals(id)).isPresent()
                || chests.findCanonicalId(id).filter(actual -> !actual.equals(id)).isPresent()) {
            throw ErrorCode.QUEST_INVALID.exception();
        }
    }
}
