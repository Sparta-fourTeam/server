package com.novaserver.user.repository;

import com.novaserver.user.entity.Wallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 유저 재화 저장소. */
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);
}
