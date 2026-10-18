package com.novaserver.shop.dto;

/** 한 회차의 진열 슬롯과 할인 가격·실제 지급 보상 스냅샷. */
public record ShopStock(int slot, int discountPercent, int price, ShopPolicy.Offer offer) {}
