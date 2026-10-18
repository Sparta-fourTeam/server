package com.novaserver.shop.repository;

import com.novaserver.shop.entity.UserGoldPackReceipt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** 계정별 영수증과 일일 상점 간 공통 요청 키 충돌 조회. */
public interface UserGoldPackReceiptRepository extends JpaRepository<UserGoldPackReceipt, Long> {
    Optional<UserGoldPackReceipt> findByUserIdAndRequestId(Long userId, String requestId);

    boolean existsByUserIdAndRequestId(Long userId, String requestId);

    @Modifying
    @Query("delete from UserGoldPackReceipt r where r.user.id=:userId")
    void deleteByUserId(Long userId);
}
