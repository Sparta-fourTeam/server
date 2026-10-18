package com.novaserver.shop.repository;

import com.novaserver.shop.entity.UserShopReceipt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** 계정별 요청 키와 최초 지급 결과 저장소. */
public interface UserShopReceiptRepository extends JpaRepository<UserShopReceipt, Long> {
    Optional<UserShopReceipt> findByUserIdAndRequestId(Long userId, String requestId);

    @Modifying
    @Query("delete from UserShopReceipt r where r.user.id=:userId")
    void deleteByUserId(Long userId);
}
