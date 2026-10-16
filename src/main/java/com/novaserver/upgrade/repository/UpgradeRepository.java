package com.novaserver.upgrade.repository;

import com.novaserver.upgrade.entity.Upgrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 강화 정도 저장소. */
public interface UpgradeRepository extends JpaRepository<Upgrade, String> {
    @Query("select count(u) from Upgrade u where u.materialItem.itemId = :itemId")
    long countByMaterialItemId(@Param("itemId") String itemId);

    @Query("select count(u) > 0 from Upgrade u where u.materialItem.itemId = :itemId")
    boolean existsByMaterialItemId(@Param("itemId") String itemId);

    @Query(
            "select count(u) > 0 from Upgrade u where u.materialItem.itemId = :itemId "
                    + "and u.upgradeId <> :excludeUpgradeId")
    boolean existsByMaterialItemIdAndUpgradeIdNot(
            @Param("itemId") String itemId, @Param("excludeUpgradeId") String excludeUpgradeId);
}
