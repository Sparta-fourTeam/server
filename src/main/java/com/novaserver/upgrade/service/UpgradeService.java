package com.novaserver.upgrade.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.skill.repository.SkillRepository;
import com.novaserver.stage.service.RewardRulesService;
import com.novaserver.tutorial.repository.TutorialRepository;
import com.novaserver.upgrade.dto.UpgradeCreateRequest;
import com.novaserver.upgrade.dto.UpgradeRequest;
import com.novaserver.upgrade.dto.UpgradeResponse;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.upgrade.entity.UpgradeEffect;
import com.novaserver.upgrade.repository.UpgradeRepository;
import com.novaserver.user.repository.UserUpgradeRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 강화 데이터 관련 API. */
@Service
@RequiredArgsConstructor
public class UpgradeService {
    private final TutorialRepository tutorials;
    private final RewardRulesService referenceLocks;
    private final UpgradeRepository upgradeRepository;
    private final UserUpgradeRepository userUpgradeRepository;
    private final ItemRepository itemRepository;
    private final SkillRepository skillRepository;

    /** 강화 데이터를 전체 조회한다. 유니티와 관리자 목록에서 함께 쓴다. */
    @Transactional(readOnly = true)
    public List<UpgradeResponse> getUpgradeList() {
        Map<String, Integer> unlocks =
                skillRepository.findUpgradeUnlockLevels().stream()
                        .collect(
                                Collectors.toMap(
                                        SkillRepository.UpgradeUnlock::getProgressionId,
                                        SkillRepository.UpgradeUnlock::getUnlockLevel));
        return upgradeRepository.findAll(Sort.by("upgradeId")).stream()
                .map(
                        upgrade -> {
                            int unlockLevel =
                                    unlocks.getOrDefault(
                                            upgrade.getUpgradeId(), upgrade.getUnlockLevel());
                            if (unlockLevel <= 0) {
                                throw ErrorCode.UPGRADE_DATA_INVALID.exception(
                                        "연결 스킬의 해금 레벨은 양수여야 합니다");
                            }
                            return UpgradeResponse.from(upgrade, unlockLevel);
                        })
                .toList();
    }

    /** 강화 한 건을 조회한다. 없으면 404를 던진다. */
    @Transactional(readOnly = true)
    public UpgradeResponse getUpgrade(String id) {
        return UpgradeResponse.from(findUpgrade(id));
    }

    /** 강화를 등록한다. 이미 있는 id면 409, 데이터가 올바르지 않으면 400을 던진다. */
    @Transactional
    public UpgradeResponse createUpgrade(UpgradeCreateRequest request) {
        // id를 직접 지정하는 엔티티라 save()가 기존 행을 덮어쓰므로, 먼저 존재 여부를 확인한다.
        if (upgradeRepository.existsById(request.getUpgradeId())) {
            throw ErrorCode.UPGRADE_DUPLICATED.exception(request.getUpgradeId());
        }
        Item materialItem = validateAndResolveMaterialItem(request, null);

        try {
            Upgrade saved =
                    upgradeRepository.saveAndFlush(
                            new Upgrade(
                                    request.getUpgradeId(),
                                    request.getMaxLevel(),
                                    resolveUnlockLevel(request.getUnlockLevel()),
                                    materialItem,
                                    request.getCostBase(),
                                    request.getCostStep(),
                                    request.getMaterialBase(),
                                    request.getMaterialStep(),
                                    request.getEffects(),
                                    request.getLevelCosts(),
                                    request.getMaterialCosts()));
            return UpgradeResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.UPGRADE_DUPLICATED.exception(request.getUpgradeId());
        }
    }

    /** 강화를 수정한다. 효과는 통째로 교체한다. 없는 id면 404를 던진다. */
    @Transactional
    public UpgradeResponse updateUpgrade(String id, UpgradeRequest request) {
        referenceLocks.lockItemReferences();
        Upgrade upgrade = findUpgrade(id);
        Item materialItem = validateAndResolveMaterialItem(request, id);
        if (tutorials.existsByRequiredUpgradeUpgradeIdAndUpgradeLevelGreaterThan(
                id, request.getMaxLevel())) {
            throw ErrorCode.TUTORIAL_LEVEL_CAP_CONFLICT.exception();
        }

        upgrade.update(
                request.getMaxLevel(),
                resolveUnlockLevel(request.getUnlockLevel()),
                materialItem,
                request.getCostBase(),
                request.getCostStep(),
                request.getMaterialBase(),
                request.getMaterialStep(),
                request.getEffects(),
                request.getLevelCosts(),
                request.getMaterialCosts());
        return UpgradeResponse.from(upgrade);
    }

    /** 강화를 삭제한다. 유저가 이미 사용 중이면 409를 던진다. */
    @Transactional
    public void deleteUpgrade(String id) {
        referenceLocks.lockItemReferences();
        Upgrade upgrade = findUpgrade(id);
        if (tutorials.existsByRequiredUpgradeUpgradeId(id)) {
            throw ErrorCode.TUTORIAL_IN_USE.exception();
        }
        if (userUpgradeRepository.existsByUpgradeId(id)) {
            throw ErrorCode.UPGRADE_IN_USE.exception(id);
        }
        upgradeRepository.delete(upgrade);
    }

    /** 강화를 id로 찾는다. 없으면 404를 던진다. */
    public Upgrade findUpgrade(String id) {
        return upgradeRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.UPGRADE_NOT_FOUND.exception(id));
    }

    /** 해금 레벨이 없으면 1로 취급한다. */
    private Integer resolveUnlockLevel(Integer unlockLevel) {
        return unlockLevel == null ? 1 : unlockLevel;
    }

    /** 재료 아이템을 materialItemId로 찾는다. id가 없으면 null, 없는 id면 404를 던진다. */
    private Item findMaterialItem(String materialItemId) {
        if (materialItemId == null) {
            return null;
        }
        return itemRepository
                .findById(materialItemId)
                .orElseThrow(() -> ErrorCode.ITEM_NOT_FOUND.exception(materialItemId));
    }

    /** 재료·효과 값이 올바른지 검증하고, 재료 아이템을 조회해서 다른 강화에 이미 연결돼 있지 않은지 확인한다. */
    private Item validateAndResolveMaterialItem(UpgradeRequest request, String excludeUpgradeId) {
        validate(request);
        Item materialItem = findMaterialItem(request.getMaterialItemId());
        validateMaterialItemNotLinked(request.getMaterialItemId(), excludeUpgradeId);
        return materialItem;
    }

    /** 효과 값의 형태와 레벨별 배열의 길이, unlockLevel이 올바른지 확인한다. */
    private void validate(UpgradeRequest request) {
        boolean hasMaterialItem = request.getMaterialItemId() != null;
        boolean hasMaterialBase =
                request.getMaterialBase() != null || request.getMaterialCosts() != null;
        if (hasMaterialItem != hasMaterialBase) {
            throw ErrorCode.UPGRADE_DATA_INVALID.exception(
                    "materialItemId와 재료 비용(materialBase 또는 materialCosts)은 함께 있어야 합니다");
        }
        validateCosts(
                request.getLevelCosts(),
                request.getMaxLevel(),
                request.getCostBase(),
                request.getCostStep(),
                "levelCosts");
        validateCosts(
                request.getMaterialCosts(),
                request.getMaxLevel(),
                request.getMaterialBase(),
                request.getMaterialStep(),
                "materialCosts");
        if (request.getEffects() == null) {
            return;
        }
        for (UpgradeEffect effect : request.getEffects()) {
            validateEffect(effect, request.getMaxLevel());
        }
    }

    private void validateCosts(
            List<Integer> costs, int maxLevel, Integer base, Integer step, String name) {
        if (costs != null) {
            if (costs.size() != maxLevel
                    || costs.stream().anyMatch(cost -> cost == null || cost <= 0)) {
                throw ErrorCode.UPGRADE_DATA_INVALID.exception(name + "는 maxLevel개 양수여야 합니다");
            }
        } else if (base != null) {
            long last = base.longValue() + (step == null ? 0L : step.longValue()) * (maxLevel - 1L);
            if (base < 0 || (step != null && step < 0) || last > Integer.MAX_VALUE) {
                throw ErrorCode.UPGRADE_DATA_INVALID.exception(name + " 선형 비용 범위가 올바르지 않습니다");
            }
        }
    }

    private void validateEffect(UpgradeEffect effect, int maxLevel) {
        if (effect.kind() == null || effect.kind().isBlank()) {
            throw ErrorCode.UPGRADE_DATA_INVALID.exception("effects.kind는 필수값입니다");
        }

        boolean hasValuePerLevel = effect.valuePerLevel() != null;
        boolean hasLevelValues = effect.levelValues() != null;
        if (hasValuePerLevel == hasLevelValues) {
            throw ErrorCode.UPGRADE_DATA_INVALID.exception(
                    "effects는 valuePerLevel과 levelValues 중 하나만 있어야 합니다");
        }

        if (hasLevelValues && effect.levelValues().size() != maxLevel) {
            throw ErrorCode.UPGRADE_DATA_INVALID.exception(
                    "effects.levelValues 길이는 maxLevel(%d)과 같아야 합니다".formatted(maxLevel));
        }
    }

    /** 하나의 재료 아이템은 하나의 강화에만 연결될 수 있다. */
    private void validateMaterialItemNotLinked(String materialItemId, String excludeUpgradeId) {
        if (materialItemId == null) {
            return;
        }
        boolean linked =
                excludeUpgradeId == null
                        ? upgradeRepository.existsByMaterialItemId(materialItemId)
                        : upgradeRepository.existsByMaterialItemIdAndUpgradeIdNot(
                                materialItemId, excludeUpgradeId);
        if (linked) {
            throw ErrorCode.ITEM_DATA_INVALID.exception(
                    "하나의 재료는 하나의 강화에만 연결되어야 합니다: " + materialItemId);
        }
    }
}
