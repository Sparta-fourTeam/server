package com.novaserver.user.repository;

import com.novaserver.user.entity.UserItem;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저별 아이템 저장소. */
public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    /** 아이템 정보를 함께 가져온다. */
    @EntityGraph(attributePaths = {"item"})
    List<UserItem> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserItem i where i.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
