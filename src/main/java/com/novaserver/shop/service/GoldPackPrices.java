package com.novaserver.shop.service;

/** 클라 ShopRules.GoldPackPrice와 같은 정수 내림 공식. */
public final class GoldPackPrices {
    private GoldPackPrices() {}

    /** 양수 골드·비율과 0~99 할인율을 검증한 뒤 호출한다. 원래 공식대로 0잼도 가능하다. */
    public static int price(int gold, int discount, int rate) {
        return (int) ((long) gold * (100 - discount) / (rate * 100L));
    }
}
