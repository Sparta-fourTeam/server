package com.novaserver.user.entity;

import com.novaserver.item.entity.Item;
import jakarta.persistence.*;
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
}
