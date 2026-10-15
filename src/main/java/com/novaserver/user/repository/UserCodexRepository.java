package com.novaserver.user.repository;

import com.novaserver.user.entity.UserCodex;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저별 도감 저장소. */
public interface UserCodexRepository extends JpaRepository<UserCodex, Long> {
    /** 도감 정보를 함께 가져온다. */
    @EntityGraph(attributePaths = {"codex"})
    List<UserCodex> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserCodex c where c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
