package com.novaserver.friend.entity;

import com.novaserver.friend.FriendStatus;
import com.novaserver.global.entity.BaseTimeEntity;
import com.novaserver.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 친구 신청과 친구 관계를 한 행으로 표현한다. PENDING이면 신청 중, ACCEPTED면 친구다. */
@Entity
@Table(
        name = "friend",
        uniqueConstraints = @UniqueConstraint(columnNames = {"requester_id", "receiver_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friend extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendStatus status;

    /** 친구 신청을 만든다. 상태는 대기 중으로 시작한다. */
    public Friend(User requester, User receiver) {
        this.requester = requester;
        this.receiver = receiver;
        this.status = FriendStatus.PENDING;
    }

    /** 신청을 수락해 친구로 만든다. */
    public void accept() {
        this.status = FriendStatus.ACCEPTED;
    }
}
