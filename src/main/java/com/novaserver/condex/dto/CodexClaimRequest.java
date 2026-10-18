package com.novaserver.condex.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 계정은 인증 토큰에서만 정하고 몬스터 ID를 자연 멱등 키로 사용한다. */
public record CodexClaimRequest(@NotNull @Positive Long monsterId) {}
