package com.novaserver.user.repository;

import com.novaserver.user.entity.UserUpgrade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 유저별 강화 정도 저장소. */
public interface UserUpgradeRepository extends JpaRepository<UserUpgrade, Long> {
    List<UserUpgrade> findByUser_Id(Long userId);
}
