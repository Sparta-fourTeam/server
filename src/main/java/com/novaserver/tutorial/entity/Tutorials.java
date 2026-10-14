package com.novaserver.tutorial.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
