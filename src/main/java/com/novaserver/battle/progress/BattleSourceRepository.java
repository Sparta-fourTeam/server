package com.novaserver.battle.progress;

import org.springframework.data.jpa.repository.JpaRepository;

/** 단일 게시 원본 조회. */
public interface BattleSourceRepository extends JpaRepository<BattleSourceEntity, Integer> {}
