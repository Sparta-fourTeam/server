package com.novaserver.user.repository;

import com.novaserver.user.entity.UserEnergyPurchaseState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 계정별 에너지 구매 날짜·횟수 저장소. 쓰기는 계정 지갑 잠금으로 직렬화한다. */
public interface UserEnergyPurchaseStateRepository
        extends JpaRepository<UserEnergyPurchaseState, Long> {
    Optional<UserEnergyPurchaseState> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserEnergyPurchaseState s where s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
