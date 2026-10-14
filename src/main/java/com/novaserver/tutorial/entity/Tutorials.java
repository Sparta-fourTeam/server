package com.novaserver.tutorial.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 튜토리얼. */
@Entity
@Table(name = "tutorials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tutorials {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
