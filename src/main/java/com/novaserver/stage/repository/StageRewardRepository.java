package com.novaserver.stage.repository;

import com.novaserver.stage.entity.StageReward;
import org.springframework.data.jpa.repository.JpaRepository;

/** 스테이지 보상 저장소. */
public interface StageRewardRepository extends JpaRepository<StageReward, Integer> {}
