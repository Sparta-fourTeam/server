package com.novaserver.user.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.skill.entity.Skill;
import com.novaserver.skill.repository.SkillRepository;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.upgrade.service.UpgradeService;
import com.novaserver.user.dto.UserUpgradeCreateRequest;
import com.novaserver.user.dto.UserUpgradeResponse;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserData;
import com.novaserver.user.entity.UserItem;
import com.novaserver.user.entity.UserUpgrade;
import com.novaserver.user.entity.UserUpgradeRequest;
import com.novaserver.user.entity.Wallet;
import com.novaserver.user.repository.UserDataRepository;
import com.novaserver.user.repository.UserItemRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserUpgradeRepository;
import com.novaserver.user.repository.UserUpgradeRequestRepository;
import com.novaserver.user.repository.WalletRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 유저의 강화 레벨업 관련 API. */
@Service
@RequiredArgsConstructor
public class UserUpgradeService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final UserItemRepository userItemRepository;
    private final UserUpgradeRepository userUpgradeRepository;
    private final UserUpgradeRequestRepository userUpgradeRequestRepository;
    private final UpgradeService upgradeService;
    private final UserDataRepository userDataRepository;
    private final SkillRepository skillRepository;
    private final PlayerLevelPolicy playerLevelPolicy;

    /** 강화 레벨을 올린다. 같은 createId로 다시 요청하면 이전 결과를 그대로 돌려줘 중복 클릭으로 인한 중복 차감을 막는다. */
    // 지갑 잠금을 기다린 뒤에도 최신 강화·요청 기록을 읽어야 하므로 이전 스냅샷을 재사용하지 않는다.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserUpgradeResponse levelUp(Long userId, UserUpgradeCreateRequest request) {
        String upgradeId = request.getUpgradeId();
        String createId = request.getCreateId();

        // 멱등성 체크
        Optional<UserUpgradeRequest> existing =
                userUpgradeRequestRepository.findByUserIdAndUpgradeIdAndCreateId(
                        userId, upgradeId, createId);
        if (existing.isPresent()) {
            return new UserUpgradeResponse(upgradeId, existing.get().getResultLevel());
        }

        // 같은 유저 동시 요청 직렬화를 위해 지갑 row를 먼저 락 잡고 조회
        final Wallet wallet =
                walletRepository
                        .findByUserIdForUpdate(userId)
                        .orElseThrow(() -> ErrorCode.WALLET_NOT_FOUND.exception(userId));

        // 잠금을 기다리는 동안 같은 요청이 완료됐을 수 있으므로 차감·최대 레벨 검사 전에 재확인한다.
        existing =
                userUpgradeRequestRepository.findByUserIdAndUpgradeIdAndCreateId(
                        userId, upgradeId, createId);
        if (existing.isPresent()) {
            return new UserUpgradeResponse(upgradeId, existing.get().getResultLevel());
        }

        // 현재 상태 조회 및 최대 레벨 체크
        Upgrade upgrade = upgradeService.findUpgrade(upgradeId);
        UserUpgrade userUpgrade =
                userUpgradeRepository.findByUserIdAndUpgradeId(userId, upgradeId).orElse(null);
        int currentLevel = userUpgrade == null ? 0 : userUpgrade.getLevel();
        if (currentLevel >= upgrade.getMaxLevel()) {
            throw ErrorCode.UPGRADE_MAX_LEVEL.exception(upgradeId);
        }

        int unlockLevel =
                skillRepository.findByProgressionId(upgradeId).stream()
                        .mapToInt(Skill::getUnlockLevel)
                        .min()
                        .orElse(upgrade.getUnlockLevel());
        if (unlockLevel <= 0) {
            throw ErrorCode.UPGRADE_DATA_INVALID.exception("연결 스킬의 해금 레벨은 양수여야 합니다");
        }
        UserData userData =
                userDataRepository
                        .findByUserId(userId)
                        .orElseThrow(() -> ErrorCode.USER_DATA_NOT_FOUND.exception(userId));
        if (!playerLevelPolicy.hasReached(userData.getExp(), unlockLevel)) {
            throw ErrorCode.UPGRADE_LOCKED.exception(upgradeId);
        }

        Integer gold =
                costAt(
                        upgrade.getLevelCosts(),
                        upgrade.getCostBase(),
                        upgrade.getCostStep(),
                        currentLevel,
                        upgrade.getMaxLevel());
        if (gold != null && wallet.getGold() < gold) {
            throw ErrorCode.WALLET_GOLD_NOT_ENOUGH.exception(upgradeId);
        }

        Integer material = null;
        UserItem userItem = null;
        if (upgrade.getMaterialItem() != null) {
            String materialItemId = upgrade.getMaterialItem().getItemId();
            material =
                    costAt(
                            upgrade.getMaterialCosts(),
                            upgrade.getMaterialBase(),
                            upgrade.getMaterialStep(),
                            currentLevel,
                            upgrade.getMaxLevel());
            if (material == null) {
                throw ErrorCode.UPGRADE_DATA_INVALID.exception("재료 비용이 없습니다");
            }
            if (material > 0) {
                userItem =
                        userItemRepository
                                .findByUserIdAndItemIdForUpdate(userId, materialItemId)
                                .orElse(null);
                if (userItem == null || userItem.getQuantity() < material) {
                    throw ErrorCode.ITEM_QUANTITY_NOT_ENOUGH.exception(materialItemId);
                }
            }
        }

        // 모든 거절 조건을 확인한 뒤에만 재화·재료·강화 레벨을 변경한다.
        if (gold != null) {
            wallet.useGold(gold);
        }
        if (userItem != null) {
            userItem.useQuantity(material);
        }

        // 레벨 반영
        User user = userRepository.getReferenceById(userId);
        if (userUpgrade == null) {
            userUpgrade = userUpgradeRepository.save(new UserUpgrade(user, upgrade, 1));
        } else {
            userUpgrade.levelUp();
        }

        try {
            userUpgradeRequestRepository.saveAndFlush(
                    new UserUpgradeRequest(
                            user, upgrade, createId, userUpgrade.getLevel(), gold, material));
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.UPGRADE_REQUEST_DUPLICATED.exception(createId);
        }

        return new UserUpgradeResponse(upgradeId, userUpgrade.getLevel());
    }

    private Integer costAt(
            List<Integer> costs, Integer base, Integer step, int level, int maxLevel) {
        if (costs != null) {
            if (costs.size() != maxLevel
                    || costs.stream().anyMatch(cost -> cost == null || cost <= 0)) {
                throw ErrorCode.UPGRADE_DATA_INVALID.exception("비용 배열은 maxLevel개 양수여야 합니다");
            }
            return costs.get(level);
        }
        if (base == null) {
            return null;
        }
        long cost = base.longValue() + (step == null ? 0L : step.longValue()) * level;
        if (cost < 0 || cost > Integer.MAX_VALUE) {
            throw ErrorCode.UPGRADE_DATA_INVALID.exception("비용 범위가 올바르지 않습니다");
        }
        return (int) cost;
    }
}
