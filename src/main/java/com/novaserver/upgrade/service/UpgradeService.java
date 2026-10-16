package com.novaserver.upgrade.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.upgrade.dto.UpgradeCreateRequest;
import com.novaserver.upgrade.dto.UpgradeRequest;
import com.novaserver.upgrade.dto.UpgradeResponse;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.upgrade.entity.UpgradeEffect;
import com.novaserver.upgrade.repository.UpgradeRepository;
import com.novaserver.user.repository.UserUpgradeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 강화 데이터 관련 API. */
@Service
@RequiredArgsConstructor
public class UpgradeService {
    private final UpgradeRepository upgradeRepository;
    private final UserUpgradeRepository userUpgradeRepository;
    private final ItemRepository itemRepository;

    /** 강화 데이터를 전체 조회한다. 유니티와 관리자 목록에서 함께 쓴다. */
    @Transactional(readOnly = true)
    public List<UpgradeResponse> getUpgradeList() {
        return upgradeRepository.findAll(Sort.by("upgradeId")).stream()
                .map(UpgradeResponse::from)
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
        validate(request);
        Item materialItem = findMaterialItem(request.getMaterialItemId());

        try {
            Upgrade saved =
                    upgradeRepository.saveAndFlush(
                            new Upgrade(
                                    request.getUpgradeId(),
                                    request.getMaxLevel(),
                                    request.getUnlockLevel(),
                                    materialItem,
                                    request.getCostBase(),
                                    request.getCostStep(),
                                    request.getMaterialBase(),
                                    request.getMaterialStep(),
                                    request.getEffects()));
            return UpgradeResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.UPGRADE_DUPLICATED.exception(request.getUpgradeId());
        }
    }

    /** 강화를 수정한다. 효과는 통째로 교체한다. 없는 id면 404를 던진다. */
    @Transactional
    public UpgradeResponse updateUpgrade(String id, UpgradeRequest request) {
        Upgrade upgrade = findUpgrade(id);
        validate(request);
        Item materialItem = findMaterialItem(request.getMaterialItemId());

        upgrade.update(
                request.getMaxLevel(),
                request.getUnlockLevel(),
                materialItem,
                request.getCostBase(),
                request.getCostStep(),
                request.getMaterialBase(),
                request.getMaterialStep(),
                request.getEffects());
        return UpgradeResponse.from(upgrade);
    }

    /** 강화를 삭제한다. 유저가 이미 사용 중이면 409를 던진다. */
    @Transactional
    public void deleteUpgrade(String id) {
        Upgrade upgrade = findUpgrade(id);
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

    /** 재료 아이템을 materialItemId로 찾는다. id가 없으면 null, 없는 id면 404를 던진다. */
    private Item findMaterialItem(String materialItemId) {
        if (materialItemId == null) {
            return null;
        }
        return itemRepository
                .findById(materialItemId)
                .orElseThrow(() -> ErrorCode.ITEM_NOT_FOUND.exception(materialItemId));
    }

    /** 효과 값의 형태와 레벨별 배열의 길이가 올바른지 확인한다. */
    private void validate(UpgradeRequest request) {
        if (request.getEffects() == null) {
            return;
        }
        for (UpgradeEffect effect : request.getEffects()) {
            validateEffect(effect, request.getMaxLevel());
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
}
