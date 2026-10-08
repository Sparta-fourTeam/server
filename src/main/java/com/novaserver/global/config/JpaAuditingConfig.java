package com.novaserver.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** JPA Auditing 설정. 엔티티의 생성·수정 시각을 자동으로 채운다. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
