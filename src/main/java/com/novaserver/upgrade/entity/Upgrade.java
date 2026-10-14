package com.novaserver.upgrade.entity;

import jakarta.persistence.*;
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

    public static Upgrade create(String id) {
        Upgrade upgrade = new Upgrade();
        upgrade.id = id;
        return upgrade;
    }
}
