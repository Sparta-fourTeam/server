package com.novaserver.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저 데이터(경험치). */
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

    /** 경험치를 지급한다. 저장 범위를 벗어나 음수로 돌아가지 않도록 0~int 최대값으로 제한한다. */
    public void addExp(int amount) {
        this.exp = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, this.exp.longValue() + amount));
    }
}
