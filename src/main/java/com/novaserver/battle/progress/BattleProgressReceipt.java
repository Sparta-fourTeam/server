package com.novaserver.battle.progress;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** 같은 보고를 다른 시각·기간의 신규 진행으로 재처리하지 않는 최초 접수 기록. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleProgressReceipt {
    @Id private String reportId;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BattleProgress progress;

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    private String payload;

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    private String ack;

    /** 원래 요청과 확정 ACK를 함께 보존한다. */
    public BattleProgressReceipt(
            String reportId, BattleProgress progress, String payload, String ack) {
        this.reportId = reportId;
        this.progress = progress;
        this.payload = payload;
        this.ack = ack;
    }
}
