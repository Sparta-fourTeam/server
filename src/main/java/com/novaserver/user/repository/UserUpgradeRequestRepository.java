package com.novaserver.user.repository;

import com.novaserver.user.entity.UserUpgradeRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저 강화 레벨업 요청 기록 저장소. */
public interface UserUpgradeRequestRepository extends JpaRepository<UserUpgradeRequest, Long> {
    @Query(
            "select r from UserUpgradeRequest r "
                    + "where r.user.id = :userId and r.upgrade.upgradeId = :upgradeId "
                    + "and r.createId = :createId")
    Optional<UserUpgradeRequest> findByUserIdAndUpgradeIdAndCreateId(
            @Param("userId") Long userId,
            @Param("upgradeId") String upgradeId,
            @Param("createId") String createId);

    @Modifying
    @Query("delete from UserUpgradeRequest r where r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
