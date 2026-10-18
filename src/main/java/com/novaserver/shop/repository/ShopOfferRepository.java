package com.novaserver.shop.repository;

import com.novaserver.shop.entity.ShopOfferDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 상품 후보와 실제 공통 보상 항목을 한 스냅샷으로 조회한다. */
public interface ShopOfferRepository extends JpaRepository<ShopOfferDefinition, String> {
    @Override
    @EntityGraph(
            attributePaths = {
                "rewardDefinition",
                "rewardDefinition.entries",
                "rewardDefinition.entries.item"
            })
    List<ShopOfferDefinition> findAll();

    long countByRewardDefinitionId(Long id);

    @Query(
            "select o.rewardDefinition.id as definitionId, "
                    + "count(o) as referenceCount from "
                    + "ShopOfferDefinition o group by o.rewardDefinition.id")
    List<DefinitionUsage> findDefinitionUsage();

    /** 상품 후보의 공통 묶음 참조 수. */
    interface DefinitionUsage {
        Long getDefinitionId();

        long getReferenceCount();
    }
}
