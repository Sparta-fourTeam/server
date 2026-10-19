package com.novaserver.battle.progress;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 관리 API가 원자적으로 게시하는 전투 원본. 서버 리소스 파일을 복제하지 않는다. */
@Entity
@Table(name = "battle_source")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleSourceEntity {
    @Id private Integer id;

    @Column(nullable = false, length = 64)
    private String sourceRevision;

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    private String stagesJson;

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    private String monstersJson;

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    private String combatJson;

    BattleSourceEntity(BattlePolicySource source) {
        id = 1;
        sourceRevision = source.revision();
        stagesJson = source.stagesJson();
        monstersJson = source.monstersJson();
        combatJson = source.combatJson();
    }
}
