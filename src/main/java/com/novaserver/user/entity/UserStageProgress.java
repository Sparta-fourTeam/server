package com.novaserver.user.entity;

import com.novaserver.stage.entity.Stage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** 유저별 스테이지 진행도. */
@Entity
@Table(
        name = "user_stage_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "stage_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStageProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @Column(nullable = false)
    private int clearRating;

    @Column(nullable = false)
    private int claimedRating;

    /** 클리어·보상 수령 등급이 0인 진행도를 만든다. */
    public UserStageProgress(User user, Stage stage) {
        this.user = user;
        this.stage = stage;
        this.clearRating = 0;
        this.claimedRating = 0;
    }

    /** 기존보다 높은 등급일 때만 클리어 등급을 갱신한다. */
    public void updateClearRating(int clearRating) {
        if (clearRating > this.clearRating) {
            this.clearRating = clearRating;
        }
    }

    /** 기존보다 높은 등급일 때만 보상 수령 등급을 갱신한다. */
    public void updateClaimedRating(int claimedRating) {
        if (claimedRating > this.claimedRating) {
            this.claimedRating = claimedRating;
        }
    }
}
