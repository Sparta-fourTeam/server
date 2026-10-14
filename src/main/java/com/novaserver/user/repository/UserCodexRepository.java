package com.novaserver.user.repository;

import com.novaserver.user.entity.UserCodex;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 유저별 도감 저장소. */
public interface UserCodexRepository extends JpaRepository<UserCodex, Long> {
    List<UserCodex> findByUser_Id(Long userId);
}
