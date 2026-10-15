package com.novaserver.user.entity;

import com.novaserver.user.Provider;
import com.novaserver.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** 유저 계정. provider에 따라 accountId가 게스트 UUID 해시 또는 소셜 ID를 뜻한다. */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "account_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(nullable = false, length = 255)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'USER'")
    private Role role = Role.USER;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 유저를 생성한다. 생성·수정 시각은 저장 시 자동으로 기록된다.
     *
     * @param provider 로그인 방식
     * @param accountId 게스트면 UUID의 SHA-256 해시, 소셜이면 ID 토큰의 sub
     */
    public User(Provider provider, String accountId) {
        this.provider = provider;
        this.accountId = accountId;
    }

    /**
     * 소셜 계정을 연동한다. provider·accountId를 통째로 교체한다.
     *
     * @param provider 연동할 소셜 provider
     * @param accountId 연동할 소셜 ID 토큰의 sub
     */
    public void linkSocialAccount(Provider provider, String accountId) {
        this.provider = provider;
        this.accountId = accountId;
    }
}
