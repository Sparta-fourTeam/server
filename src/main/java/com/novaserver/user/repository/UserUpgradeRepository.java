package com.novaserver.user.repository;

import com.novaserver.user.entity.UserUpgrade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저별 강화 정도 저장소. */
public interface UserUpgradeRepository extends JpaRepository<UserUpgrade, Long> {
    /** 강화 정보를 함께 가져온다. */
    @EntityGraph(attributePaths = {"upgrade"})
    List<UserUpgrade> findByUserId(Long userId);

    @Query(
            "select u from UserUpgrade u "
                    + "where u.user.id = :userId and u.upgrade.upgradeId = :upgradeId")
    Optional<UserUpgrade> findByUserIdAndUpgradeId(
            @Param("userId") Long userId, @Param("upgradeId") String upgradeId);

    @Modifying
    @Query("delete from UserUpgrade u where u.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("select count(u) > 0 from UserUpgrade u where u.upgrade.upgradeId = :upgradeId")
    boolean existsByUpgradeId(@Param("upgradeId") String upgradeId);
}
