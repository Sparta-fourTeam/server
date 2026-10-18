package com.novaserver.condex.dto;

import jakarta.validation.constraints.NotBlank;

/** 명시적 몬스터 ID에 연결할 등급과 기존 행의 현재 버전. */
public record CodexMonsterRequest(@NotBlank String grade, Integer expectedVersion) {}
