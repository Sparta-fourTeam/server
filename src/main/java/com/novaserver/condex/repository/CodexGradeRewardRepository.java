package com.novaserver.condex.repository;

import com.novaserver.condex.entity.CodexGradeReward;
import com.novaserver.condex.entity.MonsterGrade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 등급별 공통 보상과 그 항목을 같은 SELECT로 읽는다. */
public interface CodexGradeRewardRepository extends JpaRepository<CodexGradeReward, MonsterGrade> {
    @Override
    @EntityGraph(
            attributePaths = {
                "rewardDefinition",
                "rewardDefinition.entries",
                "rewardDefinition.entries.item"
            })
    Optional<CodexGradeReward> findById(MonsterGrade grade);

    @Override
    @EntityGraph(
            attributePaths = {
                "rewardDefinition",
                "rewardDefinition.entries",
                "rewardDefinition.entries.item"
            })
    List<CodexGradeReward> findAll();

    long countByRewardDefinitionId(Long id);

    @Query(
            "select g.rewardDefinition.id as definitionId, count(g) as referenceCount "
                    + "from CodexGradeReward g group by g.rewardDefinition.id")
    List<DefinitionUsage> findDefinitionUsage();

    /** 공통 묶음을 참조하는 도감 등급 수. */
    interface DefinitionUsage {
        Long getDefinitionId();

        long getReferenceCount();
    }
}
