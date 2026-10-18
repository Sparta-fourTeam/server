package com.novaserver.battle.progress;

import org.springframework.data.jpa.repository.JpaRepository;

/** 전투별 누적 진행 원장 저장소. 계정 잠금 아래 접근한다. */
public interface BattleProgressRepository extends JpaRepository<BattleProgress, String> {}
