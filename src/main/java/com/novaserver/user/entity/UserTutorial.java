package com.novaserver.user.entity;

import com.novaserver.tutorial.entity.Tutorials;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 계정별 튜토리얼 기록과 최초 완료 보상 영수증. 대사 본문은 저장하지 않는다. */
@Entity
@Table(
        name = "user_tutorial",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tutorial_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTutorial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutorial_id", nullable = false)
    private Tutorials tutorials;

    private Instant dialogueSeenAt;
    private Instant completedAt;

    @Column(nullable = false)
    private int gold;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Integer> items = new HashMap<>();

    @Column(nullable = false)
    private int expBefore;

    @Column(nullable = false)
    private int expAfter;

    /** 계정과 튜토리얼별 최초 상태 행을 만든다. */
    public UserTutorial(User user, Tutorials tutorials) {
        this.user = user;
        this.tutorials = tutorials;
    }

    /** 도입 대사 시청 여부를 한 번만 기록한다. 완료 조건이나 보상을 바꾸지 않는다. */
    public void recordDialogue(Instant now) {
        if (dialogueSeenAt == null) {
            dialogueSeenAt = now;
        }
    }

    /** 완료와 실제 지급 결과를 함께 확정한다. 재전송에서는 호출하지 않는다. */
    public void complete(int gold, Map<String, Integer> items, int before, int after, Instant now) {
        this.gold = gold;
        this.items.putAll(items);
        this.expBefore = before;
        this.expAfter = after;
        this.completedAt = now;
    }

    /** 클라이언트의 문자열 완료/시청 ID 목록으로 복원한다. */
    public List<String> recordedIds() {
        return Stream.of(
                        dialogueSeenAt == null ? null : tutorials.getId() + ".dialogue",
                        completedAt == null ? null : tutorials.getId())
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
