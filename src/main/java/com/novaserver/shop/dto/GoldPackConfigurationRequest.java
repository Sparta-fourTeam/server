package com.novaserver.shop.dto;

import java.util.List;

/** 관리자 전용 교환 비율·골드팩 전체 교체. 출시 데이터는 자동 입력하지 않는다. */
public record GoldPackConfigurationRequest(
        Integer expectedVersion, Integer gemGoldRate, List<Pack> packs) {
    /** 설정 누락도 검증할 수 있게 nullable 숫자로 받는 골드팩 한 행. */
    public record Pack(Integer gold, Integer discountPercent) {}
}
