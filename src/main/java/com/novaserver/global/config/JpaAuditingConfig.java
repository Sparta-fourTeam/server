package com.novaserver.global.config;

import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** JPA Auditing 설정. 엔티티의 생성·수정 시각을 자동으로 채운다. */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
public class JpaAuditingConfig {
    /** 생성·수정 시각도 기기나 JVM 기본 시간대 대신 UTC 절대 시각으로 기록한다. */
    @Bean
    public DateTimeProvider utcDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant());
    }
}
