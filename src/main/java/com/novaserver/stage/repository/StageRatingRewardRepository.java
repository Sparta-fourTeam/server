package com.novaserver.stage.repository;

import com.novaserver.stage.entity.StageRatingReward;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 별등급 최초 클리어 보상 저장소. */
public interface StageRatingRewardRepository extends JpaRepository<StageRatingReward, Long> {
    @EntityGraph(
            attributePaths = {
                "stage",
                "rewardDefinition",
                "rewardDefinition.entries",
                "rewardDefinition.entries.item"
            })
    Optional<StageRatingReward> findByStageStageIdAndRating(Integer stageId, int rating);

    boolean existsByStageStageIdAndRating(Integer stageId, int rating);

    boolean existsByStageStageId(Integer stageId);

    @EntityGraph(
            attributePaths = {
                "stage",
                "rewardDefinition",
                "rewardDefinition.entries",
                "rewardDefinition.entries.item"
            })
    List<StageRatingReward> findByStageStageIdOrderByRating(Integer stageId);

    @EntityGraph(
            attributePaths = {
                "stage",
                "rewardDefinition",
                "rewardDefinition.entries",
                "rewardDefinition.entries.item"
            })
    @Query(
            "select r from StageRatingReward r "
                    + "where r.stage.stageId = :stageId and r.rating between :from and :to "
                    + "order by r.rating")
    List<StageRatingReward> findByStageIdAndRatingBetween(
            @Param("stageId") Integer stageId, @Param("from") int from, @Param("to") int to);

    long countByRewardDefinitionId(Long definitionId);

    @Query(
            "select r.rewardDefinition.id as definitionId, count(r) as referenceCount "
                    + "from StageRatingReward r where r.rewardDefinition is not null "
                    + "group by r.rewardDefinition.id")
    List<DefinitionUsage> findDefinitionUsage();

    /** 관리자 목록의 묶음별 레이팅 참조 수 투영. */
    interface DefinitionUsage {
        Long getDefinitionId();

        long getReferenceCount();
    }
}
