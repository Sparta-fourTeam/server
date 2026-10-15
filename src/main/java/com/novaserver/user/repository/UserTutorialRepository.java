package com.novaserver.user.repository;

import com.novaserver.user.entity.UserTutorial;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저별 튜토리얼 저장소. */
public interface UserTutorialRepository extends JpaRepository<UserTutorial, Long> {
    /** 튜토리얼 정보를 함께 가져온다. */
    @EntityGraph(attributePaths = {"tutorials"})
    List<UserTutorial> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserTutorial t where t.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
