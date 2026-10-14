package com.novaserver.user.entity;

import com.novaserver.stage.entity.Stage;
import jakarta.persistence.*;
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

    public UserStageProgress(User user, Stage stage) {
        this.user = user;
        this.stage = stage;
        this.clearRating = 0;
        this.claimedRating = 0;
    }

    public void updateClearRating(int clearRating) {
        if (clearRating > this.clearRating) this.clearRating = clearRating;
    }

    public void updateClaimedRating(int claimedRating) {
        if (claimedRating > this.claimedRating) this.claimedRating = claimedRating;
    }
}
