package com.novaserver.tutorial.dto;

import com.novaserver.user.entity.UserTutorial;
import java.time.Instant;
import java.util.Map;

/** 최초 처리 결과. 재전송은 현재 잔액 대신 같은 영수증을 반환한다. */
public record TutorialReceiptResponse(
        String tutorialId,
        boolean dialogueOnly,
        int gold,
        Map<String, Integer> items,
        int expBefore,
        int expAfter,
        Instant recordedAt) {
    /** 완료 보상과 도입 대사 기록을 각각의 최초 기록으로 응답한다. */
    public static TutorialReceiptResponse from(UserTutorial t, boolean dialogue) {
        return dialogue
                ? new TutorialReceiptResponse(
                        t.getTutorials().getId() + ".dialogue",
                        true,
                        0,
                        Map.of(),
                        0,
                        0,
                        t.getDialogueSeenAt())
                : new TutorialReceiptResponse(
                        t.getTutorials().getId(),
                        false,
                        t.getGold(),
                        Map.copyOf(t.getItems()),
                        t.getExpBefore(),
                        t.getExpAfter(),
                        t.getCompletedAt());
    }
}
