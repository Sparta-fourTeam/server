package com.novaserver.quest.repository;

import com.novaserver.quest.entity.QuestChest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 상자 조건과 공통 보상 항목을 하나의 조회 스냅샷으로 읽는다. */
public interface QuestChestRepository extends JpaRepository<QuestChest, String> {
    @Query("select q.id from QuestChest q where q.id = :id")
    Optional<String> findCanonicalId(@Param("id") String id);

    @Override
    @EntityGraph(
            attributePaths = {
                "rewardDefinition",
                "rewardDefinition.entries",
                "rewardDefinition.entries.item"
            })
    Optional<QuestChest> findById(String id);

    @Override
    @EntityGraph(
            attributePaths = {
                "rewardDefinition",
                "rewardDefinition.entries",
                "rewardDefinition.entries.item"
            })
    List<QuestChest> findAll();

    long countByRewardDefinitionId(Long id);

    @Query(
            "select c.rewardDefinition.id as definitionId, count(c) "
                    + "as referenceCount from QuestChest c group by c.rewardDefinition.id")
    List<DefinitionUsage> findDefinitionUsage();

    /** 공통 묶음의 상자 참조 수. */
    interface DefinitionUsage {
        Long getDefinitionId();

        long getReferenceCount();
    }
}
