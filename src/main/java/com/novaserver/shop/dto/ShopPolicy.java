package com.novaserver.shop.dto;

import java.util.List;
import java.util.Map;

/** 하루 처음 진열할 때 동결하는 서버 정책과 실제 상품 후보. */
public record ShopPolicy(
        int version,
        int slots,
        int goldRerollsPerDay,
        int goldRerollCost,
        int adRerollsPerDay,
        List<Integer> discounts,
        List<Offer> pool) {
    /** 공통 묶음에서 읽어 확정한 골드·에너지·실제 아이템과 잼 정가. */
    public record Offer(String id, int price, int gold, int energy, Map<String, Integer> items) {}
}
