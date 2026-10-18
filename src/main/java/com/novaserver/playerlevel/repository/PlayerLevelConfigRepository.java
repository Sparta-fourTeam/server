package com.novaserver.playerlevel.repository;

import com.novaserver.playerlevel.entity.PlayerLevelConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/** 플레이어 경험치 기준 데이터 저장소. */
public interface PlayerLevelConfigRepository extends JpaRepository<PlayerLevelConfig, Long> {}
