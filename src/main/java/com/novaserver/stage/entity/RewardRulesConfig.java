package com.novaserver.stage.entity;

import com.novaserver.stage.dto.RewardRulesData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 전체 조회·교체하는 보상 기준 한 건. 아이템 카탈로그와 별도로 보관한다. */
@Entity
@Table(name = "reward_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardRulesConfig {
    public static final long CONFIG_ID = 1L;
    @Id private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private RewardRulesData data;

    /** 검증한 지급 일정과 분류를 하나의 스냅샷으로 보관한다. */
    public RewardRulesConfig(RewardRulesData data) {
        this.id = CONFIG_ID;
        this.data = data;
    }

    /** 기존 기준 전체를 교체한다. */
    public void update(RewardRulesData data) {
        this.data = data;
    }
}
