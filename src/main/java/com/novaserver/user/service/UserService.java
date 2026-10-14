package com.novaserver.user.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.dto.UserDataResponse;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserData;
import com.novaserver.user.entity.UserEnergy;
import com.novaserver.user.entity.Wallet;
import com.novaserver.user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저 상태 조회 관련 API. */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDataRepository userDataRepository;
    private final WalletRepository walletRepository;
    private final UserEnergyRepository userEnergyRepository;
    private final UserStageProgressRepository userStageProgressRepository;
    private final UserUpgradeRepository userUpgradeRepository;
    private final UserItemRepository userItemRepository;
    private final UserCodexRepository userCodexRepository;
    private final UserTutorialRepository userTutorialRepository;

    /** 가입 직후 유저의 기본 상태(경험치·재화는 0, 에너지는 최대치)를 만든다. */
    @Transactional
    public void createInitialState(User user) {
        userDataRepository.save(new UserData(user, 0));
        walletRepository.save(new Wallet(user, 0, 0));
        userEnergyRepository.save(new UserEnergy(user, UserEnergy.MAX_ENERGY));
    }

    /** 토큰의 유저 id로 유저 상태 정보를 조회한다. */
    @Transactional(readOnly = true)
    public UserDataResponse getMe(Long userId) {
        UserData userData = findUserData(userId);
        Wallet wallet = findWallet(userId);
        UserEnergy energy = findEnergy(userId);
        return UserDataResponse.of(
                userData,
                wallet,
                energy,
                userStageProgressRepository.findByUser_Id(userId),
                userUpgradeRepository.findByUser_Id(userId),
                userItemRepository.findByUser_Id(userId),
                userCodexRepository.findByUser_Id(userId),
                userTutorialRepository.findByUser_Id(userId));
    }

    private UserData findUserData(Long userId) {
        return userDataRepository
                .findByUser_Id(userId)
                .orElseThrow(() -> ErrorCode.USER_DATA_NOT_FOUND.exception(userId));
    }

    private Wallet findWallet(Long userId) {
        return walletRepository
                .findByUser_Id(userId)
                .orElseThrow(() -> ErrorCode.WALLET_NOT_FOUND.exception(userId));
    }

    private UserEnergy findEnergy(Long userId) {
        return userEnergyRepository
                .findByUser_Id(userId)
                .orElseThrow(() -> ErrorCode.ENERGY_NOT_FOUND.exception(userId));
    }
}
