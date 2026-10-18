package com.novaserver.reward.entity;

import com.novaserver.item.entity.Item;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 고정 골드·실제 아이템으로 구성된 공통 보상 묶음. 지급 영수증과 독립이다. */
@Entity
@Table(name = "reward_definition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int gold;

    @Column(nullable = false)
    private int version = 1;

    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RewardEntry> entries = new ArrayList<>();

    /** 검증된 고정 보상 묶음을 새로 만든다. */
    public RewardDefinition(String name, int gold, Map<Item, Integer> items) {
        this.name = name;
        this.gold = gold;
        items.forEach((item, amount) -> entries.add(new RewardEntry(this, item, amount)));
    }

    /** 공유 묶음을 교체하고 참조 레이팅의 다음 수령에 적용할 버전을 올린다. */
    public void replace(String name, int gold, Map<Item, Integer> items) {
        this.name = name;
        this.gold = gold;
        this.version = Math.addExact(version, 1);
        Map<String, Item> catalog = new HashMap<>();
        items.keySet().forEach(item -> catalog.put(item.getItemId(), item));
        entries.removeIf(entry -> !catalog.containsKey(entry.getItem().getItemId()));
        var existing = new HashMap<String, RewardEntry>();
        entries.forEach(entry -> existing.put(entry.getItem().getItemId(), entry));
        items.forEach(
                (item, amount) -> {
                    RewardEntry entry = existing.get(item.getItemId());
                    if (entry == null) {
                        entries.add(new RewardEntry(this, item, amount));
                    } else {
                        entry.changeQuantity(amount);
                    }
                });
    }

    /** 실제 지급·응답에 사용할 아이템별 고정 수량을 복사한다. */
    public Map<String, Integer> itemAmounts() {
        Map<String, Integer> amounts = new HashMap<>();
        entries.forEach(entry -> amounts.put(entry.getItem().getItemId(), entry.getQuantity()));
        return Map.copyOf(amounts);
    }
}
