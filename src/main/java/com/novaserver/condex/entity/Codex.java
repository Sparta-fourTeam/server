package com.novaserver.condex.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 도감(몬스터). */
@Entity
@Table(name = "codex")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Codex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
