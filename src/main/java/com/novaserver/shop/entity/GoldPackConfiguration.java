package com.novaserver.shop.entity;

import com.novaserver.shop.dto.GoldPackDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 버전·교환 비율·순서가 있는 골드팩 목록을 한 JSON 스냅샷으로 저장한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoldPackConfiguration {
    @Id private Long id = 1L;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private int gemGoldRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<GoldPackDefinition> packs;

    /** 검증된 교환 정책을 전체 교체하기 위한 스냅샷을 만든다. */
    public GoldPackConfiguration(int version, int rate, List<GoldPackDefinition> packs) {
        this.version = version;
        gemGoldRate = rate;
        this.packs = List.copyOf(packs);
    }
}
