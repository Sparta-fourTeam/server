package com.novaserver.tutorial.repository;

import com.novaserver.tutorial.entity.Tutorials;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 튜토리얼 정의·조건·지급 묶음을 한 조회 스냅샷으로 읽는다. */
public interface TutorialRepository extends JpaRepository<Tutorials, String> {
    @Override
    @EntityGraph(
            attributePaths = {
                "prerequisite",
                "startStage",
                "clearStage",
                "requiredUpgrade",
                "rewardDefinition",
                "rewardDefinition.entries",
                "rewardDefinition.entries.item"
            })
    Optional<Tutorials> findById(String id);

    @Override
    @EntityGraph(
            attributePaths = {
                "prerequisite",
                "startStage",
                "clearStage",
                "requiredUpgrade",
                "rewardDefinition",
                "rewardDefinition.entries",
                "rewardDefinition.entries.item"
            })
    List<Tutorials> findAll();

    long countByRewardDefinitionId(Long id);

    boolean existsByPrerequisiteId(String id);

    boolean existsByStartStageStageIdOrClearStageStageId(Integer startId, Integer clearId);

    boolean existsByRequiredUpgradeUpgradeId(String id);

    boolean existsByRequiredUpgradeUpgradeIdAndUpgradeLevelGreaterThan(String id, int maxLevel);

    @Query(
            "select count(t) > 0 from Tutorials t where t.minLevel > :cap "
                    + "or t.playerLevel > :cap or t.setLevel > :cap")
    boolean existsOutsidePlayerLevelCap(
            @org.springframework.data.repository.query.Param("cap") int cap);

    @Query(
            "select t.rewardDefinition.id as definitionId, count(t) as referenceCount "
                    + "from Tutorials t group by t.rewardDefinition.id")
    List<DefinitionUsage> findDefinitionUsage();

    /** 묶음별 튜토리얼 참조 수. */
    interface DefinitionUsage {
        Long getDefinitionId();

        long getReferenceCount();
    }
}
