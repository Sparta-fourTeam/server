package com.novaserver.upgrade.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 강화 정도. */
@Entity
@Table(name = "upgrade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Upgrade {
    @Id private String id;

    /** 주어진 id로 강화 정보를 만든다. */
    public static Upgrade create(String id) {
        Upgrade upgrade = new Upgrade();
        upgrade.id = id;
        return upgrade;
    }
}
