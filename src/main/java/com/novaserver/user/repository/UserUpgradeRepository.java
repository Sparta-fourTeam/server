package com.novaserver.user.repository;

import com.novaserver.user.entity.UserUpgrade;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 유저별 강화 정도 저장소. */
public interface UserUpgradeRepository extends JpaRepository<UserUpgrade, Long> {
    /** 강화 정보를 함께 가져온다. */
    @EntityGraph(attributePaths = {"upgrade"})
    List<UserUpgrade> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
