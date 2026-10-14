package com.novaserver.stage.repository;

import com.novaserver.stage.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

/** 스테이지 저장소. */
public interface StageRepository extends JpaRepository<Stage, Long> {}
