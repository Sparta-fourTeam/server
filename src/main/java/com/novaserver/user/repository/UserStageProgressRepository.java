package com.novaserver.user.repository;

import com.novaserver.user.entity.UserStageProgress;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 유저별 스테이지 진행도 저장소. */
public interface UserStageProgressRepository extends JpaRepository<UserStageProgress, Long> {
    List<UserStageProgress> findByUser_Id(Long userId);
}
