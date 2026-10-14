package com.novaserver.user.repository;

import com.novaserver.user.entity.UserItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 유저별 아이템 저장소. */
public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    List<UserItem> findByUser_Id(Long userId);
}
