package com.novaserver.user.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.upgrade.service.UpgradeService;
import com.novaserver.user.dto.UserUpgradeCreateRequest;
import com.novaserver.user.dto.UserUpgradeResponse;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserUpgrade;
import com.novaserver.user.entity.UserUpgradeRequest;
import com.novaserver.user.entity.Wallet;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserUpgradeRepository;
import com.novaserver.user.repository.UserUpgradeRequestRepository;
import com.novaserver.user.repository.WalletRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저의 강화 레벨업 관련 API. */
@Service
@RequiredArgsConstructor
public class UserUpgradeService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final UserUpgradeRepository userUpgradeRepository;
    private final UserUpgradeRequestRepository userUpgradeRequestRepository;
    private final UpgradeService upgradeService;

    /** 강화 레벨을 올린다. 같은 createId로 다시 요청하면 이전 결과를 그대로 돌려줘 중복 클릭으로 인한 중복 차감을 막는다. */
    @Transactional
    public UserUpgradeResponse levelUp(Long userId, UserUpgradeCreateRequest request) {
        String upgradeId = request.getUpgradeId();
        String createId = request.getCreateId();

        // 멱동성 체크
        Optional<UserUpgradeRequest> existing =
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

        // 골드 차감
        Integer gold = null;
        if (upgrade.getCostBase() != null) {
            int cost = upgrade.getCostBase() + upgrade.getCostStep() * currentLevel;
            Wallet wallet =
                    walletRepository
                            .findByUserId(userId)
                            .orElseThrow(() -> ErrorCode.WALLET_NOT_FOUND.exception(userId));
            if (wallet.getGold() < cost) {
                throw ErrorCode.WALLET_GOLD_NOT_ENOUGH.exception(upgradeId);
            }
            wallet.useGold(cost);
            gold = cost;
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
                    new UserUpgradeRequest(user, upgrade, createId, userUpgrade.getLevel(), gold));
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.UPGRADE_REQUEST_DUPLICATED.exception(createId);
        }

        return new UserUpgradeResponse(upgradeId, userUpgrade.getLevel());
    }
}
