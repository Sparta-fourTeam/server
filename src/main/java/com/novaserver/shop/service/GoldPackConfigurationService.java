package com.novaserver.shop.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.shop.dto.GoldPackCatalogResponse;
import com.novaserver.shop.dto.GoldPackConfigurationRequest;
import com.novaserver.shop.dto.GoldPackDefinition;
import com.novaserver.shop.entity.GoldPackConfiguration;
import com.novaserver.shop.repository.GoldPackConfigurationRepository;
import com.novaserver.stage.service.RewardRulesService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 클라와 같은 골드·할인율 정렬 검증을 적용해 서버 카탈로그를 버전 대조 후 교체한다. */
@Service
@RequiredArgsConstructor
public class GoldPackConfigurationService {
    private final GoldPackConfigurationRepository configurations;
    private final RewardRulesService locks;

    /** 관리자에게 현재 버전과 모든 계산 가격을 조회한다. */
    @Transactional(readOnly = true)
    public GoldPackCatalogResponse get() {
        return catalog(
                configurations
                        .findById(1L)
                        .orElseThrow(ErrorCode.GOLD_PACK_NOT_CONFIGURED::exception));
    }

    /** 기존 잼→골드 정책만 등록한다. 일일 한도·현금 결제 규칙은 추가하지 않는다. */
    @Transactional
    public GoldPackCatalogResponse replace(GoldPackConfigurationRequest r) {
        locks.lockItemReferences();
        var current = configurations.findById(1L);
        int version = current.map(GoldPackConfiguration::getVersion).orElse(0);
        if (r == null
                || r.expectedVersion() == null
                || r.expectedVersion() < 0
                || r.gemGoldRate() == null
                || r.gemGoldRate() <= 0
                || r.packs() == null
                || r.packs().isEmpty()) {
            throw ErrorCode.GOLD_PACK_INVALID.exception();
        }
        if (r.expectedVersion() != version) {
            throw ErrorCode.SHOP_CONFIG_CONFLICT.exception();
        }
        if (version == Integer.MAX_VALUE) {
            throw ErrorCode.GOLD_PACK_INVALID.exception();
        }
        var rows = new ArrayList<GoldPackDefinition>();
        int previousGold = 0;
        int previousDiscount = 0;
        for (var row : r.packs()) {
            if (row == null
                    || row.gold() == null
                    || row.gold() <= 0
                    || row.discountPercent() == null
                    || row.discountPercent() < 0
                    || row.discountPercent() > 99
                    || row.gold() <= previousGold
                    || row.discountPercent() < previousDiscount) {
                throw ErrorCode.GOLD_PACK_INVALID.exception();
            }
            rows.add(new GoldPackDefinition(row.gold(), row.discountPercent()));
            previousGold = row.gold();
            previousDiscount = row.discountPercent();
        }
        return catalog(
                configurations.saveAndFlush(
                        new GoldPackConfiguration(version + 1, r.gemGoldRate(), rows)));
    }

    /** 한 스냅샷에서 버전과 모든 골드팩 잼 가격을 함께 계산한다. */
    public static GoldPackCatalogResponse catalog(GoldPackConfiguration c) {
        var list = new ArrayList<GoldPackCatalogResponse.Offer>();
        for (int i = 0; i < c.getPacks().size(); i++) {
            var p = c.getPacks().get(i);
            list.add(
                    new GoldPackCatalogResponse.Offer(
                            i,
                            p.gold(),
                            p.discountPercent(),
                            GoldPackPrices.price(
                                    p.gold(), p.discountPercent(), c.getGemGoldRate())));
        }
        return new GoldPackCatalogResponse(
                true, c.getVersion(), c.getGemGoldRate(), List.copyOf(list));
    }
}
