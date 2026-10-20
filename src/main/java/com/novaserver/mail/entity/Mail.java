package com.novaserver.mail.entity;

import com.novaserver.global.entity.BaseTimeEntity;
import com.novaserver.item.entity.Item;
import com.novaserver.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 관리자가 특정 유저에게 보내는 우편. 경험치·골드·젬·에너지·아이템이 포함되면 유저가 수령해야 지급된다. */
@Entity
@Table(name = "mail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mail extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private int exp;

    @Column(nullable = false)
    private int gold;

    @Column(nullable = false)
    private int gem;

    @Column(nullable = false)
    private int energy;

    @Column(nullable = false)
    private boolean claimed = false;

    @OneToMany(mappedBy = "mail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MailItem> items = new ArrayList<>();

    /** 우편을 만든다. */
    public Mail(User user, String title, String content, int exp, int gold, int gem, int energy) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.exp = exp;
        this.gold = gold;
        this.gem = gem;
        this.energy = energy;
    }

    /** 첨부 아이템을 추가한다. */
    public void addItem(Item item, int quantity) {
        items.add(new MailItem(this, item, quantity));
    }

    /** 수령 처리한다. 중복 수령 여부는 호출하는 쪽에서 미리 확인한다. */
    public void claim() {
        this.claimed = true;
    }
}
