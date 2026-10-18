package com.novaserver.condex.repository;

import com.novaserver.condex.entity.Codex;
import com.novaserver.reward.entity.RewardDefinition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 도감용 서버 몬스터 식별자·등급과 실제 등급별 보상 조회 저장소. */
public interface CodexRepository extends JpaRepository<Codex, Long> {
    /** 등급·공통 정의·전체 항목을 하나의 SQL 스냅샷에서 읽는다. */
    @Query(
            "select distinct r from Codex c, CodexGradeReward g join g.rewardDefinition r "
                    + "left join fetch r.entries e left join fetch e.item "
                    + "where c.id = :id and c.grade = g.grade")
    Optional<RewardDefinition> findRewardForMonster(@Param("id") Long id);
}
