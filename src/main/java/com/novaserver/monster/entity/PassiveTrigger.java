package com.novaserver.monster.entity;

/** 패시브 발동 시점. Spawn은 OnDeath·Interval, Shield는 Start·OnHit만 쓴다. */
public enum PassiveTrigger {
    OnDeath(Category.Spawn),
    Interval(Category.Spawn),
    Start(Category.Shield),
    OnHit(Category.Shield);

    private final Category category;

    PassiveTrigger(Category category) {
        this.category = category;
    }

    /** 발동 시점을 쓸 수 있는 패시브 종류. */
    public enum Category {
        Spawn,
        Shield,
    }

    public Category getCategory() {
        return category;
    }
}
