package com.novaserver.quest.entity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** 퀘스트 갱신 주기. */
public enum QuestPeriod {
    DAILY,
    WEEKLY;

    /** 오늘 날짜 기준으로 이 주기의 리셋 키를 구한다. 일일은 오늘, 주간은 이번 주 월요일이다. */
    public LocalDate currentPeriodKey(LocalDate today) {
        return switch (this) {
            case DAILY -> today;
            case WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        };
    }
}
