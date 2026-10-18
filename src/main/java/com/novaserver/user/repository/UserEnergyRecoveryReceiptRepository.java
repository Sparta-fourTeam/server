package com.novaserver.user.repository;

import com.novaserver.user.EnergyRecoverySource;
import com.novaserver.user.entity.UserEnergyRecoveryReceipt;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 계정별 추가 에너지 회복 확정 영수증 저장소. */
public interface UserEnergyRecoveryReceiptRepository
        extends JpaRepository<UserEnergyRecoveryReceipt, Long> {
    Optional<UserEnergyRecoveryReceipt> findByUserIdAndCreateId(Long userId, String createId);

    long countByUserIdAndSourceAndPurchaseDay(
            Long userId, EnergyRecoverySource source, LocalDate purchaseDay);

    @Modifying
    @Query("delete from UserEnergyRecoveryReceipt r where r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
