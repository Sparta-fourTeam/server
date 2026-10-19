package com.novaserver.battle.progress;

import org.springframework.data.jpa.repository.JpaRepository;

/** 스테이지별 명시적 보고 정책 저장소. */
public interface StageProgressPolicyRepository
        extends JpaRepository<StageProgressPolicyEntity, Integer> {}
