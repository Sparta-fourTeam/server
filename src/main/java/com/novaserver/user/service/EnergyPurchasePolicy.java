package com.novaserver.user.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/** 사용자 확정 정책. UTC 시각으로부터 서울 04시 구매 기간만 계산한다. */
@Component
public class EnergyPurchasePolicy {
    public static final int ENERGY_AMOUNT = 15;
    public static final int GEM_COST = 60;
    public static final int DAILY_LIMIT = 6;
    public static final int RESET_HOUR = 4;
    private static final ZoneId PURCHASE_ZONE = ZoneId.of("Asia/Seoul");

    /** 서버의 절대 시각을 서울 04시부터 다음날 04시 직전까지의 구매 영업일로 바꾼다. */
    public LocalDate purchaseDay(Instant now) {
        return now.atZone(PURCHASE_ZONE).minusHours(RESET_HOUR).toLocalDate();
    }

    /** 현재 영업일 다음 날 서울 04시를 UTC 절대 시각으로 반환한다. */
    public Instant nextResetAt(LocalDate purchaseDay) {
        return purchaseDay.plusDays(1).atTime(RESET_HOUR, 0).atZone(PURCHASE_ZONE).toInstant();
    }
}
