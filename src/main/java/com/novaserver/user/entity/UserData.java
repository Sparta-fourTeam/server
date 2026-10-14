package com.novaserver.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_data", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer exp;

    public UserData(User user, Integer exp) {
        this.user = user;
        this.exp = exp;
    }
}
