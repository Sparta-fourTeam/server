package com.novaserver.upgrade.repository;

import com.novaserver.upgrade.entity.Upgrade;
import org.springframework.data.jpa.repository.JpaRepository;

/** 강화 정도 저장소. */
public interface UpgradeRepository extends JpaRepository<Upgrade, String> {
    boolean existsByUpgradeId(String upgradeId);
}
