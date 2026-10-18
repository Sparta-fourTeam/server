package com.novaserver.reward.entity;

import com.novaserver.item.entity.Item;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 공통 보상 묶음의 실제 아이템 항목. Item FK로 삭제·없는 참조를 차단한다. */
@Entity
@Table(
        name = "reward_entry",
        uniqueConstraints = @UniqueConstraint(columnNames = {"definition_id", "item_id"}),
        check = @CheckConstraint(name = "ck_reward_entry_quantity", constraint = "quantity > 0"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false)
    private RewardDefinition definition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int quantity;

    /** 검증된 실제 아이템과 양수 수량으로 항목을 만든다. */
    public RewardEntry(RewardDefinition definition, Item item, int quantity) {
        this.definition = definition;
        this.item = item;
        this.quantity = quantity;
    }

    public void changeQuantity(int quantity) {
        this.quantity = quantity;
    }
}
