package com.novaserver.user.service;

import com.novaserver.battle.repository.BattleRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.dto.UserDataResponse;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserData;
import com.novaserver.user.entity.UserEnergy;
import com.novaserver.user.entity.Wallet;
import com.novaserver.user.repository.UserCodexRepository;
import com.novaserver.user.repository.UserDataRepository;
import com.novaserver.user.repository.UserEnergyRepository;
import com.novaserver.user.repository.UserItemRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserStageProgressRepository;
import com.novaserver.user.repository.UserTutorialRepository;
import com.novaserver.user.repository.UserUpgradeRepository;
import com.novaserver.user.repository.UserUpgradeRequestRepository;
import com.novaserver.user.repository.WalletRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저 상태 조회 관련 API. */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;
    private final WalletRepository walletRepository;
    private final UserEnergyRepository userEnergyRepository;
    private final UserStageProgressRepository userStageProgressRepository;
    private final UserUpgradeRepository userUpgradeRepository;
    private final UserUpgradeRequestRepository userUpgradeRequestRepository;
    private final UserItemRepository userItemRepository;
    private final UserCodexRepository userCodexRepository;
    private final UserTutorialRepository userTutorialRepository;
    private final BattleRepository battleRepository;

    private final Clock clock;

    /** 가입 직후 유저의 기본 상태(경험치·재화는 0, 에너지는 최대치)를 만든다. */
    @Transactional
    public void createInitialState(User user) {
        userDataRepository.save(new UserData(user, 0));
        walletRepository.save(new Wallet(user, 0, 0));
        userEnergyRepository.save(
                new UserEnergy(user, UserEnergy.MAX_ENERGY, LocalDateTime.now(clock)));
    }

    /** 토큰의 유저 id로 유저 상태 정보를 조회한다. */
    @Transactional(readOnly = true)
    public UserDataResponse getMe(Long userId) {
        UserData userData = findUserData(userId);
        Wallet wallet = findWallet(userId);
        UserEnergy energy = findEnergy(userId);

        energy.recover(LocalDateTime.now(clock));

        return UserDataResponse.of(
                userData,
                wallet,
                energy,
                userStageProgressRepository.findByUserId(userId),
                userUpgradeRepository.findByUserId(userId),
                userItemRepository.findByUserId(userId),
                userCodexRepository.findByUserId(userId),
                userTutorialRepository.findByUserId(userId));
    }

    /** 유저와 유저에 속한 모든 데이터를 삭제한다. */
    @Transactional
    public void deleteAccount(Long userId) {
        userDataRepository.deleteByUserId(userId);
        walletRepository.deleteByUserId(userId);
        userEnergyRepository.deleteByUserId(userId);
        userStageProgressRepository.deleteByUserId(userId);
        userUpgradeRequestRepository.deleteByUserId(userId);
        userUpgradeRepository.deleteByUserId(userId);
        userItemRepository.deleteByUserId(userId);
        userCodexRepository.deleteByUserId(userId);
        userTutorialRepository.deleteByUserId(userId);
        battleRepository.deleteById(userId);
        userRepository.deleteById(userId);
    }

    /** 해당 유저가 에너지를 소모한다. */
    @Transactional
    public void useEnergy(Long userId, int amount) {
        UserEnergy energy = findEnergyForUpdate(userId);
        LocalDateTime now = LocalDateTime.now(clock);

        energy.recover(now);
        energy.consume(amount, now);
    }

    /** 해당 유저에게 에너지 보상을 지급한다. */
    @Transactional
    public void grantEnergy(Long userId, int amount) {
        UserEnergy energy = findEnergyForUpdate(userId);
        LocalDateTime now = LocalDateTime.now(clock);

        energy.recover(now);
        energy.addEnergy(amount, now);
    }

    private UserData findUserData(Long userId) {
        return userDataRepository
                .findByUserId(userId)
                .orElseThrow(() -> ErrorCode.USER_DATA_NOT_FOUND.exception(userId));
    }

    private Wallet findWallet(Long userId) {
        return walletRepository
                .findByUserId(userId)
                .orElseThrow(() -> ErrorCode.WALLET_NOT_FOUND.exception(userId));
    }

    private UserEnergy findEnergy(Long userId) {
        return userEnergyRepository
                .findByUserId(userId)
                .orElseThrow(() -> ErrorCode.ENERGY_NOT_FOUND.exception(userId));
    }

    private UserEnergy findEnergyForUpdate(Long userId) {
        try {
            return userEnergyRepository
                    .findByUserIdForUpdate(userId)
                    .orElseThrow(() -> ErrorCode.ENERGY_NOT_FOUND.exception(userId));
        } catch (PessimisticLockingFailureException e) {
            throw ErrorCode.ENERGY_LOCK_CONFLICT.exception();
        }
    }
}
