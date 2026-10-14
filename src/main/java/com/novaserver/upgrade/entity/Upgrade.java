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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public static Upgrade create() {
        return new Upgrade();
    }
}
