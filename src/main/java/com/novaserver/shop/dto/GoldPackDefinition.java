package com.novaserver.shop.dto;

/** 서버 골드팩 정의. 클라 GoldPackDefinition의 골드·할인율 의미를 따른다. */
public record GoldPackDefinition(int gold, int discountPercent) {}
