package com.novaserver.stage.dto;

import java.util.Map;

/** 배틀 종료 시 즉시 지급되는 기본 보상 계산 결과. */
public record ClearRewardResult(int exp, int gold, Map<String, Integer> items) {}
