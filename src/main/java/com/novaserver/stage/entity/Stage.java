package com.novaserver.stage.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stage")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public static Stage create() {
        return new Stage();
    }
}
