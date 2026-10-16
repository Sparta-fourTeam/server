package com.novaserver.user.repository;

import com.novaserver.user.entity.UserUpgrade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 유저별 강화 정도 저장소.
 */
public interface UserUpgradeRepository extends JpaRepository<UserUpgrade, Long> {
    /**
     * 강화 정보를 함께 가져온다.
     */
    @EntityGraph(attributePaths = {"upgrade"})
    List<UserUpgrade> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserUpgrade u where u.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("select count(u) > 0 from UserUpgrade u where u.upgrade.upgradeId = :upgradeId")
    boolean existsByUpgradeId(@Param("upgradeId") String upgradeId);
}
