package com.novaserver.battle.entity;

import com.novaserver.battle.dto.BattleEndRequest;
import com.novaserver.stage.dto.ClearRewardResult;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 활성 세션과 독립적으로 계정 삭제까지 보존하는 완료 영수증. */
@Entity
@Table(name = "battle_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleResult {
    @Id private String battleId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer stageId;

    @Column(nullable = false)
    private int clearedWave;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private int exp;

    @Column(nullable = false)
    private int gold;

    @Column(nullable = false)
    private int clearRating;

    @Column(nullable = false)
    private Instant completedAt;

    @ElementCollection
    @CollectionTable(name = "battle_result_item", joinColumns = @JoinColumn(name = "battle_id"))
    @MapKeyColumn(name = "item_id")
    @Column(name = "amount", nullable = false)
    private Map<String, Integer> items = new HashMap<>();

    /** 최초 지급한 보상과 당시 진척을 복원 가능한 값으로 저장한다. */
    public BattleResult(
            Long userId,
            BattleEndRequest request,
            ClearRewardResult reward,
            int clearRating,
            Instant completedAt) {
        this.battleId = request.getBattleId();
        this.userId = userId;
        this.stageId = request.getStageId();
        this.clearedWave = request.getClearedWave();
        this.rating = request.getRating();
        this.exp = reward.exp();
        this.gold = reward.gold();
        this.items.putAll(reward.items());
        this.clearRating = clearRating;
        this.completedAt = completedAt;
    }

    /** 같은 battleId에 다른 결과를 덮어쓰려는 재제출을 구분한다. */
    public boolean matches(BattleEndRequest request) {
        return stageId.equals(request.getStageId())
                && clearedWave == request.getClearedWave()
                && rating == request.getRating();
    }
}
