package com.novaserver.user.repository;

import com.novaserver.user.entity.StageRatingClaim;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 계정별 별 보상 최초 수령 영수증 저장소. */
public interface StageRatingClaimRepository extends JpaRepository<StageRatingClaim, Long> {
    Optional<StageRatingClaim> findByUserIdAndCreateId(Long userId, String createId);

    @Modifying
    @Query("delete from StageRatingClaim c where c.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
