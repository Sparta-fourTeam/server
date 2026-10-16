package com.novaserver.stage.repository;

import com.novaserver.stage.entity.StageRatingReward;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 별등급 최초 클리어 보상 저장소. */
public interface StageRatingRewardRepository extends JpaRepository<StageRatingReward, Long> {
    Optional<StageRatingReward> findByStageStageIdAndRating(Integer stageId, int rating);

    boolean existsByStageStageIdAndRating(Integer stageId, int rating);

    List<StageRatingReward> findByStageStageIdOrderByRating(Integer stageId);

    @Query(
            "select r from StageRatingReward r "
                    + "where r.stage.stageId = :stageId and r.rating between :from and :to "
                    + "order by r.rating")
    List<StageRatingReward> findByStageIdAndRatingBetween(
            @Param("stageId") Integer stageId, @Param("from") int from, @Param("to") int to);
}
