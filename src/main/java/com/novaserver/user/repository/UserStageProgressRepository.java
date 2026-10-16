package com.novaserver.user.repository;

import com.novaserver.user.entity.UserStageProgress;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저별 스테이지 진행도 저장소. */
public interface UserStageProgressRepository extends JpaRepository<UserStageProgress, Long> {
    /** 스테이지를 함께 가져온다. */
    @EntityGraph(attributePaths = {"stage"})
    List<UserStageProgress> findByUserId(Long userId);

    @Query(
            "select s from UserStageProgress s "
                    + "where s.user.id = :userId and s.stage.stageId = :stageId")
    Optional<UserStageProgress> findByUserIdAndStageId(
            @Param("userId") Long userId, @Param("stageId") Integer stageId);

    /** 별등급 보상 수령 시 동시 요청으로 중복 지급되지 않도록 락을 잡고 조회한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select s from UserStageProgress s "
                    + "where s.user.id = :userId and s.stage.stageId = :stageId")
    Optional<UserStageProgress> findByUserIdAndStageIdForUpdate(
            @Param("userId") Long userId, @Param("stageId") Integer stageId);

    @Modifying
    @Query("delete from UserStageProgress s where s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
