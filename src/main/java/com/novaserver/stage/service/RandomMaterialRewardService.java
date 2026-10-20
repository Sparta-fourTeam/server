package com.novaserver.stage.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.skill.repository.SkillRepository;
import com.novaserver.stage.dto.ClearRewardResult;
import com.novaserver.stage.dto.RewardRulesData;
import com.novaserver.upgrade.repository.UpgradeRepository;
import com.novaserver.user.repository.UserDataRepository;
import com.novaserver.user.service.PlayerLevelPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 별도 추첨 ID를 지급 전 계정 레벨에서 열린 실제 재료로 확정한다. */
@Component
@RequiredArgsConstructor
public class RandomMaterialRewardService {
    private final ItemRepository items;
    private final SkillRepository skills;
    private final UpgradeRepository upgrades;
    private final UserDataRepository users;
    private final PlayerLevelPolicy levels;

    /** 같은 재료의 중복 추첨과 고정 보상을 합산하며 실제 ID만 반환한다. */
    public ClearRewardResult resolve(Long userId, ClearRewardResult reward, RewardRulesData rules) {
        return resolve(userId, reward, rules, ThreadLocalRandom.current());
    }

    ClearRewardResult resolve(
            Long userId, ClearRewardResult reward, RewardRulesData rules, RandomGenerator random) {
        Map<String, Integer> resolved = new HashMap<>();
        Map<String, List<String>> candidatesByKind = new HashMap<>();
        try {
            for (var entry : reward.items().entrySet()) {
                var rule = rules.items().get(entry.getKey());
                if (rule == null || entry.getValue() == null || entry.getValue() <= 0) {
                    throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
                }
                if (!rule.random()) {
                    resolved.merge(entry.getKey(), entry.getValue(), Math::addExact);
                    continue;
                }
                var candidates =
                        candidatesByKind.computeIfAbsent(
                                rule.kind(), kind -> candidates(userId, kind, rules));
                if (candidates.isEmpty()) {
                    throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
                }
                // 클라이언트와 동일하게 최대 두 번 균등 복원 추출하고 나머지는 앞 선택에 준다.
                int quantity = entry.getValue();
                int picks = Math.min(2, quantity);
                for (int pick = 0; pick < picks; pick++) {
                    String id = candidates.get(random.nextInt(candidates.size()));
                    int amount = quantity / picks + (pick < quantity % picks ? 1 : 0);
                    resolved.merge(id, amount, Math::addExact);
                }
            }
        } catch (ArithmeticException error) {
            throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
        }
        return new ClearRewardResult(
                reward.exp(), reward.gold(), resolved, reward.threeStarBonusExp());
    }

    private List<String> candidates(Long userId, String kind, RewardRulesData rules) {
        int exp =
                users.findByUserId(userId)
                        .orElseThrow(() -> ErrorCode.USER_DATA_NOT_FOUND.exception(userId))
                        .getExp();
        var ids =
                rules.items().entrySet().stream()
                        .filter(
                                entry ->
                                        !entry.getValue().random()
                                                && entry.getValue().kind().equals(kind))
                        .map(Map.Entry::getKey)
                        .sorted()
                        .toList();
        var catalog =
                items.findAllById(ids).stream()
                        .collect(Collectors.toMap(Item::getItemId, item -> item));
        Map<String, String> targets = new HashMap<>();
        if (kind.equals("EquipmentMaterial")) {
            // 장비 재료는 TargetId를 두지 않고 강화(Upgrade)의 재료로 대상을 찾는다 (1재료 → 1강화)
            var linked = upgrades.findByMaterialItemIds(ids);
            if (linked.size() != ids.size()) {
                throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
            }
            linked.forEach(
                    upgrade ->
                            targets.put(
                                    upgrade.getMaterialItem().getItemId(), upgrade.getUpgradeId()));
            if (targets.size() != ids.size()) {
                throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
            }
        } else {
            for (String id : ids) {
                Item item = catalog.get(id);
                if (item == null || item.getTargetId() == null || item.getTargetId().isBlank()) {
                    throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
                }
                targets.put(id, item.getTargetId());
            }
        }
        Map<String, Integer> unlockLevels;
        if (kind.equals("SkillMaterial")) {
            List<Long> skillIds;
            try {
                skillIds = targets.values().stream().map(Long::valueOf).distinct().toList();
            } catch (NumberFormatException error) {
                throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
            }
            var skillLevels =
                    skills.findAllById(skillIds).stream()
                            .collect(
                                    Collectors.toMap(
                                            skill -> skill.getId().toString(),
                                            skill -> skill.getUnlockLevel()));
            unlockLevels = new HashMap<>();
            targets.forEach(
                    (id, target) ->
                            unlockLevels.put(id, skillLevels.get(Long.valueOf(target).toString())));
        } else if (kind.equals("EquipmentMaterial")) {
            var linkedLevels =
                    skills.findUpgradeUnlockLevels().stream()
                            .collect(
                                    Collectors.toMap(
                                            SkillRepository.UpgradeUnlock::getProgressionId,
                                            SkillRepository.UpgradeUnlock::getUnlockLevel));
            var equipmentLevels =
                    upgrades.findAllById(targets.values()).stream()
                            .collect(
                                    Collectors.toMap(
                                            upgrade -> upgrade.getUpgradeId(),
                                            upgrade ->
                                                    linkedLevels.getOrDefault(
                                                            upgrade.getUpgradeId(),
                                                            upgrade.getUnlockLevel())));
            unlockLevels = new HashMap<>();
            targets.forEach((id, target) -> unlockLevels.put(id, equipmentLevels.get(target)));
        } else {
            throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
        }
        int level = levels.levelAt(exp);
        return ids.stream()
                .filter(
                        id -> {
                            Integer unlockLevel = unlockLevels.get(id);
                            if (unlockLevel == null || unlockLevel <= 0) {
                                throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
                            }
                            return level >= unlockLevel;
                        })
                .toList();
    }
}
