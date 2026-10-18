package com.novaserver.shop.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.shop.dto.ShopPolicy;
import com.novaserver.shop.dto.ShopReceiptResponse;
import com.novaserver.shop.dto.ShopRerollRequest;
import com.novaserver.shop.dto.ShopStateResponse;
import com.novaserver.shop.entity.ShopOfferDefinition;
import com.novaserver.shop.entity.UserShopReceipt;
import com.novaserver.shop.entity.UserShopState;
import com.novaserver.shop.repository.ShopConfigurationRepository;
import com.novaserver.shop.repository.ShopOfferRepository;
import com.novaserver.shop.repository.UserGoldPackReceiptRepository;
import com.novaserver.shop.repository.UserShopReceiptRepository;
import com.novaserver.shop.repository.UserShopStateRepository;
import com.novaserver.stage.service.RewardRulesService;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserEnergyRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.WalletRepository;
import com.novaserver.user.service.EnergyPurchasePolicy;
import com.novaserver.user.service.UserRewardService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 계정별 진열·가격·보상을 동결하고 실제 차감·지급·영수증을 원자적으로 확정한다. */
@Service
@RequiredArgsConstructor
public class UserShopService {
    private final UserRepository accounts;
    private final WalletRepository wallets;
    private final UserEnergyRepository energies;
    private final UserRewardService rewards;
    private final ShopConfigurationRepository configurations;
    private final ShopOfferRepository offers;
    private final UserShopStateRepository states;
    private final UserShopReceiptRepository receipts;
    private final RewardRulesService locks;
    private final EnergyPurchasePolicy days;
    private final ShopRoller roller;
    private final Clock clock;
    private final UserGoldPackReceiptRepository goldPackReceipts;

    /** 설정 참조 잠금 아래 최초 진열을 만들며 계정 조회의 다른 일관 읽기보다 먼저 호출한다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShopStateResponse snapshot(Long userId) {
        var user = account(userId);
        locks.lockItemReferences();
        var now = now();
        var state = current(user, now);
        return state == null ? unavailable(now) : response(state);
    }

    /** 조회한 날짜·회차의 슬롯만 구매한다. 같은 키는 날짜가 지나도 최초 영수증을 돌려준다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShopReceiptResponse buy(
            Long userId, String requestId, String dayKey, int reroll, int slot) {
        final var user = account(userId);
        var prior = prior(userId, requestId, "BUY", dayKey, reroll, slot);
        if (prior != null) {
            return prior;
        }
        final var wallet =
                wallets.findByUserIdForUpdate(userId)
                        .orElseThrow(() -> ErrorCode.WALLET_NOT_FOUND.exception(userId));
        locks.lockItemReferences();
        var now = now();
        validateDay(dayKey, now);
        final var state = required(current(user, now));
        checkRevision(state, reroll);
        if (slot < 0 || slot >= state.getOffers().size()) {
            throw ErrorCode.SHOP_INVALID.exception();
        }
        if (state.getPurchased().contains(slot)) {
            throw ErrorCode.SHOP_SOLD_OUT.exception();
        }
        var stock = state.getOffers().get(slot);
        if (wallet.getGem() < stock.price()) {
            throw ErrorCode.WALLET_GEM_NOT_ENOUGH.exception();
        }
        wallet.useGem(stock.price());
        rewards.grant(userId, 0, stock.offer().gold(), stock.offer().items());
        grantEnergy(userId, stock.offer().energy(), now);
        state.buy(slot);
        states.saveAndFlush(state);
        return save(
                new UserShopReceipt(
                        user,
                        requestId,
                        "BUY",
                        dayKey,
                        reroll,
                        slot,
                        0,
                        stock.price(),
                        stock.offer().gold(),
                        stock.offer().energy(),
                        stock.offer().items(),
                        response(state),
                        now));
    }

    /** 골드 한도·가격은 동결한 서버 정책으로 검사한다. 광고는 검증 공급자 없이는 성공시키지 않는다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShopReceiptResponse reroll(
            Long userId, String requestId, String dayKey, int reroll, ShopRerollRequest.Kind kind) {
        if (kind == null) {
            throw ErrorCode.SHOP_INVALID.exception();
        }
        if (kind == ShopRerollRequest.Kind.AD) {
            throw ErrorCode.SHOP_AD_UNAVAILABLE.exception();
        }
        final var user = account(userId);
        var prior = prior(userId, requestId, "GOLD_REROLL", dayKey, reroll, null);
        if (prior != null) {
            return prior;
        }
        final var wallet =
                wallets.findByUserIdForUpdate(userId)
                        .orElseThrow(() -> ErrorCode.WALLET_NOT_FOUND.exception(userId));
        locks.lockItemReferences();
        var now = now();
        validateDay(dayKey, now);
        final var state = required(current(user, now));
        checkRevision(state, reroll);
        if (state.getGoldUsed() >= state.getPolicy().goldRerollsPerDay()) {
            throw ErrorCode.SHOP_REROLL_LIMIT.exception();
        }
        int cost = state.getPolicy().goldRerollCost();
        if (wallet.getGold() < cost) {
            throw ErrorCode.WALLET_GOLD_NOT_ENOUGH.exception(userId);
        }
        var next = roller.roll(state.getPolicy());
        wallet.useGold(cost);
        state.reroll(next);
        states.saveAndFlush(state);
        return save(
                new UserShopReceipt(
                        user,
                        requestId,
                        "GOLD_REROLL",
                        dayKey,
                        reroll,
                        null,
                        cost,
                        0,
                        0,
                        0,
                        Map.of(),
                        response(state),
                        now));
    }

    /** 계정 삭제 트랜잭션에서 영수증과 현재 진열의 아이템 연결도 함께 제거한다. */
    @Transactional
    public void deleteRecords(Long userId) {
        goldPackReceipts.deleteByUserId(userId);
        receipts.deleteByUserId(userId);
        states.findByUserId(userId).ifPresent(states::delete);
        states.flush();
    }

    private ShopReceiptResponse prior(
            Long userId, String key, String operation, String day, int reroll, Integer slot) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{1,64}") || reroll < 0) {
            throw ErrorCode.SHOP_INVALID.exception();
        }
        if (goldPackReceipts.existsByUserIdAndRequestId(userId, key)) {
            throw ErrorCode.SHOP_REQUEST_CONFLICT.exception();
        }
        var prior = receipts.findByUserIdAndRequestId(userId, key);
        if (prior.isEmpty()) {
            return null;
        }
        if (!prior.get().matches(key, operation, day, reroll, slot)) {
            throw ErrorCode.SHOP_REQUEST_CONFLICT.exception();
        }
        return ShopReceiptResponse.from(prior.get());
    }

    private UserShopState current(User user, Instant now) {
        var day = days.purchaseDay(now);
        var existing = states.findByUserId(user.getId());
        if (existing.isPresent() && existing.get().getShopDay().equals(day)) {
            return existing.get();
        }
        var c = configurations.findById(1L);
        if (c.isEmpty()) {
            return null;
        }
        var pool =
                offers.findAll().stream()
                        .sorted(Comparator.comparing(ShopOfferDefinition::getId))
                        .toList();
        var config = c.get();
        var policy =
                new ShopPolicy(
                        config.getVersion(),
                        config.getSlots(),
                        config.getGoldRerollsPerDay(),
                        config.getGoldRerollCost(),
                        config.getAdRerollsPerDay(),
                        List.copyOf(config.getDiscounts()),
                        pool.stream()
                                .map(
                                        o ->
                                                new ShopPolicy.Offer(
                                                        o.getId(),
                                                        o.getPrice(),
                                                        o.getRewardDefinition().getGold(),
                                                        o.getEnergy(),
                                                        o.getRewardDefinition().itemAmounts()))
                                .toList());
        var refs = new LinkedHashMap<String, Item>();
        pool.forEach(
                o ->
                        o.getRewardDefinition()
                                .getEntries()
                                .forEach(e -> refs.put(e.getItem().getItemId(), e.getItem())));
        var state = existing.orElseGet(() -> new UserShopState(user));
        state.beginDay(day, policy, roller.roll(policy), new ArrayList<>(refs.values()));
        return states.saveAndFlush(state);
    }

    private ShopStateResponse response(UserShopState s) {
        return new ShopStateResponse(
                true,
                key(s.getShopDay()),
                days.nextResetAt(s.getShopDay()),
                s.getPolicy().version(),
                s.rerollIndex(),
                s.getGoldUsed(),
                Math.max(0, s.getPolicy().goldRerollsPerDay() - s.getGoldUsed()),
                s.getPolicy().goldRerollCost(),
                s.getAdUsed(),
                false,
                s.getPurchased().stream().sorted().toList(),
                List.copyOf(s.getOffers()));
    }

    private ShopStateResponse unavailable(Instant now) {
        var day = days.purchaseDay(now);
        return new ShopStateResponse(
                false,
                key(day),
                days.nextResetAt(day),
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                List.of(),
                List.of());
    }

    private void grantEnergy(Long userId, int amount, Instant now) {
        if (amount == 0) {
            return;
        }
        var energy =
                energies.findByUserIdForUpdate(userId)
                        .orElseThrow(() -> ErrorCode.ENERGY_NOT_FOUND.exception(userId));
        energy.recover(now);
        if ((long) energy.getEnergyStored() + amount > Integer.MAX_VALUE) {
            throw ErrorCode.ENERGY_STORAGE_LIMIT.exception();
        }
        energy.addEnergy(amount, now);
    }

    private ShopReceiptResponse save(UserShopReceipt receipt) {
        return ShopReceiptResponse.from(receipts.saveAndFlush(receipt));
    }

    private User account(Long id) {
        return accounts.findByIdForUpdate(id).orElseThrow(ErrorCode.ACCOUNT_DELETED::exception);
    }

    private UserShopState required(UserShopState s) {
        if (s == null) {
            throw ErrorCode.SHOP_NOT_CONFIGURED.exception();
        }
        return s;
    }

    private void checkRevision(UserShopState s, int reroll) {
        if (s.rerollIndex() != reroll) {
            throw ErrorCode.SHOP_STALE_STOCK.exception();
        }
    }

    private void validateDay(String key, Instant now) {
        if (!key(days.purchaseDay(now)).equals(key)) {
            throw ErrorCode.SHOP_STALE_STOCK.exception();
        }
    }

    private String key(LocalDate day) {
        return day.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
