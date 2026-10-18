package com.novaserver.shop.dto;

import java.util.List;

/** 서버가 계산한 잼 가격과 실제 지급 골드 목록. 구매 한도는 기존 클라처럼 별도로 두지 않는다. */
public record GoldPackCatalogResponse(
        boolean available, int version, int gemGoldRate, List<Offer> packs) {
    /** 현재 카탈로그 버전에 속한 순서 번호·골드·할인율·잼 가격. */
    public record Offer(int index, int gold, int discountPercent, int gemPrice) {}
}
