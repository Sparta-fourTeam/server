package com.novaserver.shop.dto;

import java.time.Instant;
import java.util.List;

/** 서버가 저장한 현재 일일 진열과 구매·재추첨 상태. 앱은 이 상품을 그대로 표시한다. */
public record ShopStateResponse(
        boolean available,
        String dayKey,
        Instant nextResetAt,
        int configVersion,
        int rerollIndex,
        int goldUsed,
        int goldLeft,
        int goldRerollCost,
        int adUsed,
        boolean adAvailable,
        List<Integer> purchased,
        List<ShopStock> offers) {}
