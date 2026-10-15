package com.novaserver.user.repository;

import com.novaserver.user.entity.UserStageProgress;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저별 스테이지 진행도 저장소. */
public interface UserStageProgressRepository extends JpaRepository<UserStageProgress, Long> {
    /** 스테이지를 함께 가져온다. */
    @EntityGraph(attributePaths = {"stage"})
    List<UserStageProgress> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserStageProgress s where s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
