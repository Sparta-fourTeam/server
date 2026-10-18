package com.novaserver.reward.repository;

import com.novaserver.reward.entity.RewardDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 공통 고정 보상 묶음과 실제 아이템 참조 저장소. */
public interface RewardDefinitionRepository extends JpaRepository<RewardDefinition, Long> {
    @Override
    @EntityGraph(attributePaths = {"entries", "entries.item"})
    Optional<RewardDefinition> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"entries", "entries.item"})
    List<RewardDefinition> findAll();

    @Query("select count(e) > 0 from RewardEntry e where e.item.itemId = :itemId")
    boolean existsByItemId(@Param("itemId") String itemId);
}
