package com.novaserver.shop.dto;

import java.util.List;

/** 관리자 전용 서버 상점 정책과 공통 보상 연결의 전체 교체 입력. */
public record ShopConfigurationRequest(
        Integer expectedVersion,
        Integer slots,
        Integer goldRerollsPerDay,
        Integer goldRerollCost,
        Integer adRerollsPerDay,
        List<Integer> discounts,
        List<Offer> offers) {
    /** 운영자만 정하는 상품 ID·잼 정가·에너지·기존 공통 보상 묶음 참조. */
    public record Offer(String id, Integer price, Integer energy, Long rewardDefinitionId) {}
}
