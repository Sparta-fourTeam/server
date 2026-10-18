package com.novaserver.battle.progress;

import com.novaserver.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** 활성 세션이 교체·포기돼도 인정한 진행과 보고 재전송 근거를 유지한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleProgress {
    @Id private String battleId;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    private Integer stageId;
    private int policyVersion;

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    private String policy;

    private long revision;
    private int killedMonsters;
    private int completedWaves;
    private int reachedWave;

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    private String encounters = "[]";

    private Instant startedAt;
    private boolean closed;

    /** 실제 발급 세션과 정책 개정으로 진행 원장을 연다. */
    public BattleProgress(
            User user,
            String battleId,
            Integer stageId,
            StageProgressPolicyEntity policy,
            Instant startedAt) {
        this.user = user;
        this.battleId = battleId;
        this.stageId = stageId;
        this.policy = policy.getDefinition();
        this.policyVersion = policy.getVersion();
        this.startedAt = startedAt;
    }

    /** 검증을 통과한 누적 값만 저장한다. */
    public void accept(ProgressReport report, String encounters) {
        revision = Math.addExact(revision, 1);
        killedMonsters = report.killedMonsters();
        completedWaves = report.completedWaves();
        reachedWave = report.reachedWave();
        this.encounters = encounters;
    }

    public void close() {
        closed = true;
    }
}
