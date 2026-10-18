package com.novaserver.shop.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.reward.service.RewardDefinitionService;
import com.novaserver.shop.dto.ShopConfigurationRequest;
import com.novaserver.shop.dto.ShopConfigurationResponse;
import com.novaserver.shop.entity.ShopConfiguration;
import com.novaserver.shop.entity.ShopOfferDefinition;
import com.novaserver.shop.repository.ShopConfigurationRepository;
import com.novaserver.shop.repository.ShopOfferRepository;
import com.novaserver.stage.service.RewardRulesService;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 서버 상점 정책과 공통 보상 연결을 버전 대조 후 전체 교체한다. */
@Service
@RequiredArgsConstructor
public class ShopConfigurationService {
    private final ShopConfigurationRepository configurations;
    private final ShopOfferRepository offers;
    private final RewardDefinitionService rewards;
    private final RewardRulesService locks;

    @Transactional(readOnly = true)
    public ShopConfigurationResponse get() {
        var c = configurations.findById(1L).orElseThrow(ErrorCode.SHOP_NOT_CONFIGURED::exception);
        return response(c);
    }

    /** 기존 계정의 오늘 동결 정책은 바꾸지 않고 이후 최초 진열에 적용한다. */
    @Transactional
    public ShopConfigurationResponse replace(ShopConfigurationRequest r) {
        locks.lockItemReferences();
        var existing = configurations.findById(1L);
        int version = existing.map(ShopConfiguration::getVersion).orElse(0);
        validate(r);
        if (r.expectedVersion() != version) {
            throw ErrorCode.SHOP_CONFIG_CONFLICT.exception();
        }
        if (version == Integer.MAX_VALUE) {
            throw ErrorCode.SHOP_INVALID.exception();
        }
        var old =
                offers.findAll().stream()
                        .collect(Collectors.toMap(ShopOfferDefinition::getId, o -> o));
        var keep = new HashSet<String>();
        for (var row : r.offers()) {
            if (row == null
                    || row.id() == null
                    || !row.id().matches("[a-z0-9_.-]{1,64}")
                    || !keep.add(row.id())
                    || row.price() == null
                    || row.price() < 0
                    || row.energy() == null
                    || row.energy() < 0
                    || row.rewardDefinitionId() == null
                    || row.rewardDefinitionId() < 1) {
                throw ErrorCode.SHOP_INVALID.exception();
            }
            var reward = rewards.findDefinition(row.rewardDefinitionId());
            if (reward.getGold() == 0 && reward.itemAmounts().isEmpty() && row.energy() == 0) {
                throw ErrorCode.SHOP_INVALID.exception();
            }
            var offer =
                    old.getOrDefault(
                            row.id(),
                            new ShopOfferDefinition(row.id(), row.price(), row.energy(), reward));
            offer.configure(row.price(), row.energy(), reward);
            offers.save(offer);
        }
        old.values().stream().filter(o -> !keep.contains(o.getId())).forEach(offers::delete);
        offers.flush();
        var saved =
                configurations.saveAndFlush(
                        new ShopConfiguration(
                                version + 1,
                                r.slots(),
                                r.goldRerollsPerDay(),
                                r.goldRerollCost(),
                                r.adRerollsPerDay(),
                                r.discounts()));
        return response(saved);
    }

    private void validate(ShopConfigurationRequest r) {
        if (r == null
                || r.expectedVersion() == null
                || r.expectedVersion() < 0
                || r.slots() == null
                || r.slots() < 1
                || r.slots() > 100
                || r.goldRerollsPerDay() == null
                || r.goldRerollsPerDay() < 0
                || r.goldRerollsPerDay() > 100
                || r.adRerollsPerDay() == null
                || r.adRerollsPerDay() < 0
                || r.adRerollsPerDay() > 100
                || r.goldRerollCost() == null
                || r.goldRerollCost() < 0
                || r.discounts() == null
                || r.discounts().isEmpty()
                || r.discounts().size() > 100
                || r.discounts().stream().anyMatch(d -> d == null || d < 0 || d > 100)
                || r.offers() == null
                || r.offers().size() < r.slots()
                || r.offers().size() > 500) {
            throw ErrorCode.SHOP_INVALID.exception();
        }
    }

    private ShopConfigurationResponse response(ShopConfiguration c) {
        return new ShopConfigurationResponse(
                c.getVersion(),
                c.getSlots(),
                c.getGoldRerollsPerDay(),
                c.getGoldRerollCost(),
                c.getAdRerollsPerDay(),
                List.copyOf(c.getDiscounts()),
                offers.findAll().stream()
                        .sorted(Comparator.comparing(ShopOfferDefinition::getId))
                        .map(
                                o ->
                                        new ShopConfigurationRequest.Offer(
                                                o.getId(),
                                                o.getPrice(),
                                                o.getEnergy(),
                                                o.getRewardDefinition().getId()))
                        .toList());
    }
}
