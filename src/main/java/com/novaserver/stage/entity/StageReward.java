package com.novaserver.stage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 완료 보상. stageId로 Stage와 묶인다. */
@Getter
@Entity
@Table(name = "stage_reward")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StageReward {
    @Id private Integer stageId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @Column(nullable = false)
    private int baseExp;

    @Column(nullable = false)
    private int threeStarBonusExp;

    @Column(nullable = false)
    private int finalCoin;

    /** 20웨이브 완료 시 지급할 아이템별 총수량 목록. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<StageRewardItem> items = new ArrayList<>();

    /**
     * 스테이지 보상을 생성한다.
     *
     * @param stage 대상 스테이지
     * @param baseExp 웨이브를 1개 이상 완료하면 주는 경험치
     * @param threeStarBonusExp 별 3개 클리어 때 추가로 주는 경험치
     * @param finalCoin 20웨이브 완료 시 총 코인
     * @param items 20웨이브 완료 시 지급할 아이템별 총수량 목록
     */
    public StageReward(
            Stage stage,
            int baseExp,
            int threeStarBonusExp,
            int finalCoin,
            List<StageRewardItem> items) {
        this.stage = stage;
        this.baseExp = baseExp;
        this.threeStarBonusExp = threeStarBonusExp;
        this.finalCoin = finalCoin;
        this.items = items != null ? items : new ArrayList<>();
    }

    /**
     * 보상 내용을 수정한다. items는 통째로 교체한다.
     *
     * @param baseExp 웨이브를 1개 이상 완료하면 주는 경험치
     * @param threeStarBonusExp 별 3개 클리어 때 추가로 주는 경험치
     * @param finalCoin 20웨이브 완료 시 총 코인
     * @param items 20웨이브 완료 시 지급할 아이템별 총수량 목록
     */
    public void update(
            int baseExp, int threeStarBonusExp, int finalCoin, List<StageRewardItem> items) {
        this.baseExp = baseExp;
        this.threeStarBonusExp = threeStarBonusExp;
        this.finalCoin = finalCoin;
        this.items = items != null ? items : new ArrayList<>();
    }
}
