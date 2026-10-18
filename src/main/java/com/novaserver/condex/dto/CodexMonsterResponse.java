package com.novaserver.condex.dto;

import com.novaserver.condex.entity.Codex;

/** 전체 전투 능력치 대신 도감 보상에 필요한 ID·등급만 제공한다. */
public record CodexMonsterResponse(Long monsterId, String grade, int version) {
    /** 저장된 도감 카탈로그를 응답으로 복사한다. */
    public static CodexMonsterResponse from(Codex row) {
        return new CodexMonsterResponse(
                row.getId(), row.getGrade().getExternalName(), row.getVersion());
    }
}
