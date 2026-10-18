package com.novaserver.tutorial.dto;

import com.novaserver.user.entity.UserTutorial;
import java.time.Instant;

/** 계정별 도입 대사·완료·보상 수령 복원 상태. 완료와 지급은 항상 함께 확정된다. */
public record TutorialStateResponse(
        String tutorialId,
        boolean dialogueSeen,
        boolean completed,
        boolean rewardClaimed,
        Instant dialogueSeenAt,
        Instant completedAt) {
    /** 계정에 확정 저장된 상태만 노출한다. */
    public static TutorialStateResponse from(UserTutorial t) {
        return new TutorialStateResponse(
                t.getTutorials().getId(),
                t.getDialogueSeenAt() != null,
                t.getCompletedAt() != null,
                t.getCompletedAt() != null,
                t.getDialogueSeenAt(),
                t.getCompletedAt());
    }
}
