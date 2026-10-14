package com.novaserver.user.repository;

import com.novaserver.user.entity.UserTutorial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 유저별 튜토리얼 저장소. */
public interface UserTutorialRepository extends JpaRepository<UserTutorial, Long> {
    List<UserTutorial> findByUser_Id(Long userId);
}
