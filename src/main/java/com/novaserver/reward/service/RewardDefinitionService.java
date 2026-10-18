package com.novaserver.reward.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.reward.dto.RewardDefinitionRequest;
import com.novaserver.reward.dto.RewardDefinitionResponse;
import com.novaserver.reward.entity.RewardDefinition;
import com.novaserver.reward.repository.RewardDefinitionRepository;
import com.novaserver.stage.repository.StageRatingRewardRepository;
import com.novaserver.stage.service.RewardRulesService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공통 고정 보상 정의 관리. 웨이브·랜덤 규칙과 계정 지급은 변경하지 않는다. */
@Service
@RequiredArgsConstructor
public class RewardDefinitionService {
    private final RewardDefinitionRepository definitions;
    private final ItemRepository items;
    private final StageRatingRewardRepository ratings;
    private final RewardRulesService referenceLocks;

    /** 전체 묶음과 참조 수를 일괄 조회한다. */
    @Transactional(readOnly = true)
    public List<RewardDefinitionResponse> list() {
        var usage =
                ratings.findDefinitionUsage().stream()
                        .collect(
                                Collectors.toMap(
                                        StageRatingRewardRepository.DefinitionUsage
                                                ::getDefinitionId,
                                        StageRatingRewardRepository.DefinitionUsage
                                                ::getReferenceCount));
        return definitions.findAll().stream()
                .map(
                        definition ->
                                RewardDefinitionResponse.from(
                                        definition, usage.getOrDefault(definition.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public RewardDefinitionResponse get(Long id) {
        return response(findDefinition(id));
    }

    /** 아이템 참조와 수량을 검증한 새 묶음을 만든다. */
    @Transactional
    public RewardDefinitionResponse create(RewardDefinitionRequest request) {
        referenceLocks.lockItemReferences();
        var resolved = validate(request);
        return response(
                definitions.saveAndFlush(
                        new RewardDefinition(request.name(), request.gold(), resolved)));
    }

    /** 공유 묶음 전체를 현재 버전과 대조해 교체한다. 과거 영수증은 수정하지 않는다. */
    @Transactional
    public RewardDefinitionResponse update(Long id, RewardDefinitionRequest request) {
        referenceLocks.lockItemReferences();
        var definition = findDefinition(id);
        if (request.expectedVersion() == null || request.expectedVersion() < 1) {
            throw ErrorCode.REWARD_DEFINITION_INVALID.exception();
        }
        if (request.expectedVersion() != definition.getVersion()) {
            throw ErrorCode.REWARD_DEFINITION_VERSION_CONFLICT.exception();
        }
        var resolved = validate(request);
        if (definition.getVersion() == Integer.MAX_VALUE) {
            throw ErrorCode.REWARD_DEFINITION_INVALID.exception();
        }
        definition.replace(request.name(), request.gold(), resolved);
        definitions.flush();
        return response(definition);
    }

    /** 참조 중인 묶음은 거절하고 미참조 묶음과 항목만 함께 삭제한다. */
    @Transactional
    public void delete(Long id) {
        referenceLocks.lockItemReferences();
        var definition = findDefinition(id);
        if (ratings.countByRewardDefinitionId(id) > 0) {
            throw ErrorCode.REWARD_DEFINITION_IN_USE.exception(id);
        }
        definitions.delete(definition);
        definitions.flush();
    }

    /** 레이팅이 참조할 기존 묶음을 읽는다. 변경 경로는 참조 잠금 안에서 호출한다. */
    public RewardDefinition findDefinition(Long id) {
        return definitions
                .findById(id)
                .orElseThrow(() -> ErrorCode.REWARD_DEFINITION_NOT_FOUND.exception(id));
    }

    private RewardDefinitionResponse response(RewardDefinition definition) {
        return RewardDefinitionResponse.from(
                definition, ratings.countByRewardDefinitionId(definition.getId()));
    }

    private Map<Item, Integer> validate(RewardDefinitionRequest request) {
        if (request == null
                || request.name() == null
                || request.name().isBlank()
                || request.name().length() > 100
                || request.gold() == null
                || request.gold() < 0) {
            throw ErrorCode.REWARD_DEFINITION_INVALID.exception();
        }
        var amounts = request.items() == null ? Map.<String, Integer>of() : request.items();
        if (amounts.entrySet().stream()
                .anyMatch(
                        entry ->
                                entry.getKey() == null
                                        || entry.getKey().isBlank()
                                        || entry.getValue() == null
                                        || entry.getValue() <= 0)) {
            throw ErrorCode.REWARD_DEFINITION_INVALID.exception();
        }
        var catalog =
                items.findAllById(amounts.keySet()).stream()
                        .collect(Collectors.toMap(Item::getItemId, item -> item));
        Map<Item, Integer> resolved = new HashMap<>();
        amounts.forEach(
                (id, amount) -> {
                    Item item = catalog.get(id);
                    if (item == null) {
                        throw ErrorCode.ITEM_NOT_FOUND.exception(id);
                    }
                    resolved.put(item, amount);
                });
        return resolved;
    }
}
