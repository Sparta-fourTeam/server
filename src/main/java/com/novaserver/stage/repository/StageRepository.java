package com.novaserver.stage.repository;

import com.novaserver.stage.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

/** 스테이지 저장소. */
public interface StageRepository extends JpaRepository<Stage, Integer> {
    /** 주어진 stageId 다음 순서의 스테이지를 찾는다. 마지막 스테이지면 비어 있다. */
    java.util.Optional<Stage> findFirstByStageIdGreaterThanOrderByStageIdAsc(Integer stageId);
}
