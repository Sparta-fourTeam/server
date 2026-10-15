package com.novaserver.user.repository;

import com.novaserver.user.entity.Wallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저 재화 저장소. */
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);

    @Modifying
    @Query("delete from Wallet w where w.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
