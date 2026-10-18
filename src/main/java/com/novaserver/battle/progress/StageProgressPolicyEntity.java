package com.novaserver.battle.progress;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 등록된 서버 스폰 보고 정책. 활성 전투는 열 때 이 원문을 따로 동결한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StageProgressPolicyEntity {
    @Id private Integer stageId;
    private int version;

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    private String definition;

    /** 최초 정책을 등록한다. */
    public StageProgressPolicyEntity(Integer stageId, String definition) {
        this.stageId = stageId;
        replace(definition);
    }

    /** 동결된 기존 전투를 바꾸지 않고 이후 전투용 개정을 올린다. */
    public void replace(String definition) {
        version = Math.addExact(version, 1);
        this.definition = definition;
    }
}
