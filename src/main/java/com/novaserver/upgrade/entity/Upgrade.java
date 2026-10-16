package com.novaserver.upgrade.entity;

import com.novaserver.item.entity.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 강화 정도. */
@Getter
@Entity
@Table(name = "upgrade")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Upgrade {
    @Id private String upgradeId;

    @Column(nullable = false)
    private Integer maxLevel;

    @ColumnDefault("1")
    @Column(nullable = false)
    private Integer unlockLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_item_id")
    private Item materialItem;

    private Integer costBase;

    private Integer costStep;

    private Integer materialBase;

    private Integer materialStep;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<UpgradeEffect> effects;

    /** 강화 정보를 생성한다. */
    public Upgrade(
            String upgradeId,
            Integer maxLevel,
            Integer unlockLevel,
            Item materialItem,
            Integer costBase,
            Integer costStep,
            Integer materialBase,
            Integer materialStep,
            List<UpgradeEffect> effects) {
        this.upgradeId = upgradeId;
        update(
                maxLevel,
                unlockLevel,
                materialItem,
                costBase,
                costStep,
                materialBase,
                materialStep,
                effects);
    }

    /** 강화 정보를 수정한다. */
    public void update(
            Integer maxLevel,
            Integer unlockLevel,
            Item materialItem,
            Integer costBase,
            Integer costStep,
            Integer materialBase,
            Integer materialStep,
            List<UpgradeEffect> effects) {
        this.maxLevel = maxLevel;
        this.unlockLevel = unlockLevel;
        this.materialItem = materialItem;
        this.costBase = costBase;
        this.costStep = costStep;
        this.materialBase = materialBase;
        this.materialStep = materialStep;
        this.effects = effects;
    }
}
