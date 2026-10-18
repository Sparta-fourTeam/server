package com.novaserver.shop.dto;

import java.util.List;

/** 관리자 서버 정책 조회 결과. 현재 계정에 이미 동결된 정책과 구분한다. */
public record ShopConfigurationResponse(
        int version,
        int slots,
        int goldRerollsPerDay,
        int goldRerollCost,
        int adRerollsPerDay,
        List<Integer> discounts,
        List<ShopConfigurationRequest.Offer> offers) {}
