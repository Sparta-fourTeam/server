package com.novaserver.shop.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.shop.dto.GoldPackCatalogResponse;
import com.novaserver.shop.dto.GoldPackReceiptResponse;
import com.novaserver.shop.entity.UserGoldPackReceipt;
import com.novaserver.shop.repository.GoldPackConfigurationRepository;
import com.novaserver.shop.repository.UserGoldPackReceiptRepository;
import com.novaserver.shop.repository.UserShopReceiptRepository;
import com.novaserver.stage.service.RewardRulesService;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.WalletRepository;
import com.novaserver.user.service.UserRewardService;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 현금 결제 없이 보유 잼을 서버 카탈로그의 골드로 교환한다. */
@Service
@RequiredArgsConstructor
public class UserGoldPackService {
    private final UserRepository accounts;
    private final WalletRepository wallets;
    private final GoldPackConfigurationRepository configurations;
    private final UserGoldPackReceiptRepository receipts;
    private final UserShopReceiptRepository dailyReceipts;
    private final UserRewardService rewards;
    private final RewardRulesService locks;
    private final Clock clock;

    /** 설정이 없으면 비활성 목록을 반환하고 구매는 성공시키지 않는다. */
    @Transactional(readOnly = true)
    public GoldPackCatalogResponse catalog(Long userId) {
        accounts.findById(userId).orElseThrow(ErrorCode.ACCOUNT_DELETED::exception);
        return configurations
                .findById(1L)
                .map(GoldPackConfigurationService::catalog)
                .orElseGet(() -> new GoldPackCatalogResponse(false, 0, 0, List.of()));
    }

    /** 같은 키의 최초 영수증을 우선 복원하고 새 요청만 버전·잔액 검증 후 원자적으로 교환한다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public GoldPackReceiptResponse buy(Long userId, String key, int version, int index) {
        final var user =
                accounts.findByIdForUpdate(userId)
                        .orElseThrow(ErrorCode.ACCOUNT_DELETED::exception);
        if (key == null || !key.matches("[A-Za-z0-9_-]{1,64}") || version < 1 || index < 0) {
            throw ErrorCode.GOLD_PACK_INVALID.exception();
        }
        var prior = receipts.findByUserIdAndRequestId(userId, key);
        if (prior.isPresent()) {
            if (!prior.get().matches(key, version, index)) {
                throw ErrorCode.SHOP_REQUEST_CONFLICT.exception();
            }
            return GoldPackReceiptResponse.from(prior.get());
        }
        if (dailyReceipts.findByUserIdAndRequestId(userId, key).isPresent()) {
            throw ErrorCode.SHOP_REQUEST_CONFLICT.exception();
        }
        final var wallet =
                wallets.findByUserIdForUpdate(userId)
                        .orElseThrow(() -> ErrorCode.WALLET_NOT_FOUND.exception(userId));
        locks.lockItemReferences();
        var c =
                configurations
                        .findById(1L)
                        .orElseThrow(ErrorCode.GOLD_PACK_NOT_CONFIGURED::exception);
        if (c.getVersion() != version) {
            throw ErrorCode.GOLD_PACK_CATALOG_CHANGED.exception();
        }
        if (index >= c.getPacks().size()) {
            throw ErrorCode.GOLD_PACK_INVALID.exception();
        }
        var pack = c.getPacks().get(index);
        int price = GoldPackPrices.price(pack.gold(), pack.discountPercent(), c.getGemGoldRate());
        if (wallet.getGem() < price) {
            throw ErrorCode.WALLET_GEM_NOT_ENOUGH.exception();
        }
        var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        wallet.useGem(price);
        rewards.grant(userId, 0, pack.gold(), Map.of());
        return GoldPackReceiptResponse.from(
                receipts.saveAndFlush(
                        new UserGoldPackReceipt(
                                user,
                                key,
                                version,
                                index,
                                pack.gold(),
                                price,
                                c.getGemGoldRate(),
                                pack.discountPercent(),
                                now)));
    }
}
