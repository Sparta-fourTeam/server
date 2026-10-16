package com.novaserver.item.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 아이템(재료·상자). */
@Getter
@Entity
@Table(name = "item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {
    @Id private String itemId;

    @Column(name = "target_id")
    private String targetId;

    /** 아이템을 생성한다. */
    public Item(String itemId, String targetId) {
        this.itemId = itemId;
        this.targetId = targetId;
    }

    /** 아이템을 수정한다. */
    public void update(String targetId) {
        this.targetId = targetId;
    }
}
