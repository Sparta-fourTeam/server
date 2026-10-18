package com.novaserver.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 추가 에너지 회복 경로. 광고는 검증 서비스 연결 전까지 지급하지 않는다. */
public enum EnergyRecoverySource {
    @JsonProperty("Purchase")
    PURCHASE,
    @JsonProperty("Ad")
    AD
}
