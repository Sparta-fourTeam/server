package com.novaserver.user.entity;

import com.novaserver.item.entity.Item;
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

/** 유저별 아이템. */
@Entity
@Table(
        name = "user_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false)
    private int quantity;

    /** 유저가 처음 아이템을 얻을 때 만든다. */
    public UserItem(User user, Item item, int quantity) {
        this.user = user;
        this.item = item;
        this.quantity = quantity;
    }

    /** 아이템 수량을 사용한다. 충분한지는 호출하는 쪽에서 미리 확인한다. */
    public void useQuantity(int amount) {
        this.quantity -= amount;
    }

    /** 아이템 수량을 지급한다. */
    public void addQuantity(int amount) {
        this.quantity += amount;
    }
}
