package com.novaserver.shop.entity;

import com.novaserver.item.entity.Item;
import com.novaserver.shop.dto.ShopPolicy;
import com.novaserver.shop.dto.ShopStock;
import com.novaserver.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 계정의 현재 날짜·회차·진열과 동결 정책. 실제 아이템 FK도 유지해 지급 전 삭제를 막는다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class UserShopState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate shopDay;

    @Column(nullable = false)
    private int goldUsed;

    @Column(nullable = false)
    private int adUsed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private ShopPolicy policy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<ShopStock> offers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Set<Integer> purchased = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "user_shop_item_ref",
            joinColumns = @JoinColumn(name = "shop_state_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id"))
    private List<Item> catalogItems = new ArrayList<>();

    /** 계정의 최초 진열 저장 행을 만든다. */
    public UserShopState(User user) {
        this.user = user;
    }

    /** 날짜가 바뀌면 서버 정책·실제 상품 후보와 참조를 함께 새로 확정한다. */
    public void beginDay(
            LocalDate day, ShopPolicy policy, List<ShopStock> offers, List<Item> refs) {
        shopDay = day;
        this.policy = policy;
        this.offers = List.copyOf(offers);
        goldUsed = 0;
        adUsed = 0;
        purchased = new HashSet<>();
        catalogItems.clear();
        catalogItems.addAll(refs);
    }

    /** 최초 구매 성공에서만 품절 슬롯을 기록한다. */
    public void buy(int slot) {
        purchased = new HashSet<>(purchased);
        purchased.add(slot);
    }

    /** 골드 재추첨을 소비하고 새 진열과 빈 구매 목록을 함께 저장한다. */
    public void reroll(List<ShopStock> offers) {
        goldUsed++;
        this.offers = List.copyOf(offers);
        purchased = new HashSet<>();
    }

    public int rerollIndex() {
        return goldUsed + adUsed;
    }
}
