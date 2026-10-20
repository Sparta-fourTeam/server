package com.novaserver.mail.entity;

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

/** 우편에 첨부된 아이템 항목. */
@Entity
@Table(
        name = "mail_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mail_id", "item_id"}),
        check = @CheckConstraint(name = "ck_mail_item_quantity", constraint = "quantity > 0"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MailItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mail_id", nullable = false)
    private Mail mail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int quantity;

    /** 첨부 아이템 항목을 만든다. */
    public MailItem(Mail mail, Item item, int quantity) {
        this.mail = mail;
        this.item = item;
        this.quantity = quantity;
    }
}
