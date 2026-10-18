package com.novaserver.stage.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.stage.dto.RewardRulesData;
import com.novaserver.stage.dto.RewardRulesResponse;
import com.novaserver.stage.entity.RewardRulesConfig;
import com.novaserver.stage.repository.RewardRulesConfigRepository;
import com.novaserver.stage.repository.StageRewardRepository;
import com.novaserver.version.entity.DataTable;
import com.novaserver.version.repository.DataVersionRepository;
import com.novaserver.version.service.DataVersionService;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 보상 기준 전체 조회·교체. 미설정 상태를 코드 기본값으로 대체하지 않는다. */
@Service
@RequiredArgsConstructor
public class RewardRulesService {
    private final RewardRulesConfigRepository repository;
    private final DataVersionRepository versions;
    private final DataVersionService versionService;
    private final ItemRepository items;
    private final StageRewardRepository stageRewards;

    /** 지급 일정과 분류를 같은 DB 스냅샷으로 읽는다. */
    @Transactional(readOnly = true)
    public RewardRulesData getRules() {
        var data =
                repository
                        .findById(RewardRulesConfig.CONFIG_ID)
                        .orElseThrow(ErrorCode.REWARD_RULES_NOT_CONFIGURED::exception)
                        .getData();
        if (data == null || !data.valid()) {
            throw ErrorCode.REWARD_RULES_NOT_CONFIGURED.exception();
        }
        return data;
    }

    /** 기준과 버전을 같은 읽기 스냅샷으로 반환한다. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RewardRulesResponse getConfig() {
        var data = getRules();
        int version =
                versions.findById(DataTable.REWARD_RULES.getKey())
                        .orElseThrow(
                                () -> ErrorCode.DATA_VERSION_ROW_MISSING.exception("RewardRules"))
                        .getVersion();
        return new RewardRulesResponse(data.schedules(), data.items(), version);
    }

    /** 스테이지 편집도 기준 교체와 같은 잠금을 사용한다. */
    @Transactional
    public RewardRulesData lockRules() {
        lockVersion();
        return getRules();
    }

    /** 실제 아이템을 참조하는 보상 규칙이 남아 있으면 삭제를 거절한다. */
    @Transactional
    public void checkItemRemovable(String itemId) {
        lockVersion();
        if (repository.existsById(RewardRulesConfig.CONFIG_ID)) {
            var rule = getRules().items().get(itemId);
            if (rule != null && !rule.random()) {
                throw ErrorCode.ITEM_LINKED_TO_REWARD_RULES.exception(itemId);
            }
        }
    }

    /** 검증한 기준을 한 번에 저장하고 테이블·전체 버전을 함께 증가시킨다. */
    @Transactional
    public RewardRulesResponse saveConfig(RewardRulesData data) {
        if (data == null || !data.valid()) {
            throw ErrorCode.REWARD_RULES_INVALID.exception();
        }
        lockVersion();
        var fixedIds =
                data.items().entrySet().stream()
                        .filter(entry -> !entry.getValue().random())
                        .map(entry -> entry.getKey())
                        .toList();
        var catalog =
                items.findAllById(fixedIds).stream()
                        .map(Item::getItemId)
                        .collect(Collectors.toSet());
        for (String id : fixedIds) {
            if (!catalog.contains(id)) {
                throw ErrorCode.ITEM_NOT_FOUND.exception(id);
            }
        }
        for (var reward : stageRewards.findAll()) {
            if (reward.getFinalCoin() < data.schedules().get("Coin").minimumFinal()) {
                throw ErrorCode.STAGE_REWARD_BALANCE_INVALID.exception();
            }
            for (var item : reward.getItems()) {
                var schedule =
                        data.itemSchedule(item.itemId())
                                .orElseThrow(ErrorCode.REWARD_RULES_INVALID::exception);
                if (item.finalAmount() < schedule.minimumFinal()) {
                    throw ErrorCode.REWARD_RULES_INVALID.exception();
                }
            }
        }
        var config =
                repository
                        .findById(RewardRulesConfig.CONFIG_ID)
                        .orElseGet(() -> new RewardRulesConfig(data));
        config.update(data);
        repository.saveAndFlush(config);
        versionService.increase(DataTable.REWARD_RULES.getKey());
        return getConfig();
    }

    private void lockVersion() {
        versions.findByTableNameForUpdate(DataTable.REWARD_RULES.getKey())
                .orElseThrow(() -> ErrorCode.DATA_VERSION_ROW_MISSING.exception("RewardRules"));
    }
}
