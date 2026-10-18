package com.novaserver.shop.repository;

import com.novaserver.shop.entity.UserShopState;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 계정의 현재 상점과 지급 가능한 아이템 참조 저장소. */
public interface UserShopStateRepository extends JpaRepository<UserShopState, Long> {
    @EntityGraph(attributePaths = "catalogItems")
    Optional<UserShopState> findByUserId(Long userId);

    @Query("select count(s)>0 from UserShopState s join s.catalogItems i where i.itemId=:itemId")
    boolean existsByCatalogItem(@Param("itemId") String itemId);
}
