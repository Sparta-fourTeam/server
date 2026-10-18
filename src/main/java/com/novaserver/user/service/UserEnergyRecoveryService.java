package com.novaserver.user.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.EnergyRecoverySource;
import com.novaserver.user.dto.EnergyRecoveryRequest;
import com.novaserver.user.dto.EnergyRecoveryResponse;
import com.novaserver.user.entity.UserEnergy;
import com.novaserver.user.entity.UserEnergyPurchaseState;
import com.novaserver.user.entity.UserEnergyRecoveryReceipt;
import com.novaserver.user.entity.Wallet;
import com.novaserver.user.repository.UserEnergyPurchaseStateRepository;
import com.novaserver.user.repository.UserEnergyRecoveryReceiptRepository;
import com.novaserver.user.repository.UserEnergyRepository;
import com.novaserver.user.repository.WalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 유료 에너지 구매·멱등 재요청. 광고는 서버 검증이 준비되기 전까지 지급하지 않는다. */
@Service
@RequiredArgsConstructor
public class UserEnergyRecoveryService {
    private final UserEnergyRecoveryReceiptRepository receipts;
    private final UserEnergyPurchaseStateRepository purchaseStates;
    private final WalletRepository wallets;
    private final UserEnergyRepository energies;
    private final UserService users;
    private final EnergyPurchasePolicy policy;
    private final Clock clock;

    /** 지갑 잠금으로 일일 한도와 차감·지급·영수증을 원자적으로 처리한다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EnergyRecoveryResponse recover(Long userId, EnergyRecoveryRequest request) {
        var prior = receipts.findByUserIdAndCreateId(userId, request.createId());
        if (prior.isPresent()) {
            return replay(prior.get(), request);
        }
        if (request.source() != EnergyRecoverySource.PURCHASE) {
            throw ErrorCode.AD_REWARD_UNAVAILABLE.exception();
        }

        final Wallet wallet =
                wallets.findByUserIdForUpdate(userId)
                        .orElseThrow(() -> ErrorCode.WALLET_NOT_FOUND.exception(userId));
        // 잠금을 기다리는 동안 완료된 같은 요청을 한도/차감보다 먼저 재확인한다.
        prior = receipts.findByUserIdAndCreateId(userId, request.createId());
        if (prior.isPresent()) {
            return replay(prior.get(), request);
        }
        // MySQL/H2 timestamp(6)에 저장된 재요청 결과도 최초 응답과 정확히 같게 만든다.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        LocalDate day = policy.purchaseDay(now);
        UserEnergyPurchaseState state =
                purchaseStates.findByUserId(userId).orElseGet(() -> restoreState(wallet, day));
        state.beginPeriod(day);
        if (state.getPurchaseCount() >= EnergyPurchasePolicy.DAILY_LIMIT) {
            throw ErrorCode.ENERGY_PURCHASE_LIMIT.exception();
        }
        if (wallet.getGem() < EnergyPurchasePolicy.GEM_COST) {
            throw ErrorCode.WALLET_GEM_NOT_ENOUGH.exception();
        }
        UserEnergy energy = lockEnergy(userId);
        // 보상은 자연회복 상한을 넘을 수 있지만 int 초과로 구매분이 소실돼서는 안 된다.
        if ((long) energy.getEnergyStored() + EnergyPurchasePolicy.ENERGY_AMOUNT
                > Integer.MAX_VALUE) {
            throw ErrorCode.ENERGY_STORAGE_LIMIT.exception();
        }

        wallet.useGem(EnergyPurchasePolicy.GEM_COST);
        users.grantEnergy(userId, EnergyPurchasePolicy.ENERGY_AMOUNT);
        state.recordPurchase();
        var receipt =
                new UserEnergyRecoveryReceipt(
                        wallet.getUser(),
                        request.createId(),
                        request.source(),
                        EnergyPurchasePolicy.ENERGY_AMOUNT,
                        EnergyPurchasePolicy.GEM_COST,
                        day,
                        state.getPurchaseCount(),
                        now);
        receipts.saveAndFlush(receipt);
        return EnergyRecoveryResponse.from(receipt);
    }

    private UserEnergyPurchaseState restoreState(Wallet wallet, LocalDate day) {
        // 이전 초안의 영수증이 존재하면 최초 상태 생성 때만 당일 횟수를 복원한다.
        // 정상 구매는 이후 이 상태 행만 사용하며 과거 영수증은 삭제하지 않는다.
        long count =
                receipts.countByUserIdAndSourceAndPurchaseDay(
                        wallet.getUser().getId(), EnergyRecoverySource.PURCHASE, day);
        return purchaseStates.save(
                new UserEnergyPurchaseState(wallet.getUser(), day, Math.toIntExact(count)));
    }

    private EnergyRecoveryResponse replay(
            UserEnergyRecoveryReceipt prior, EnergyRecoveryRequest request) {
        if (prior.getSource() != request.source()) {
            throw ErrorCode.ENERGY_REQUEST_CONFLICT.exception();
        }
        return EnergyRecoveryResponse.from(prior);
    }

    private UserEnergy lockEnergy(Long userId) {
        try {
            return energies.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> ErrorCode.ENERGY_NOT_FOUND.exception(userId));
        } catch (PessimisticLockingFailureException error) {
            throw ErrorCode.ENERGY_LOCK_CONFLICT.exception();
        }
    }
}
