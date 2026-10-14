package com.novaserver.condex.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "codex")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Codex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
