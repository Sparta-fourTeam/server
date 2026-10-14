package com.novaserver.user.repository;

import com.novaserver.user.entity.UserData;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 유저 데이터 저장소. */
public interface UserDataRepository extends JpaRepository<UserData, Long> {
    Optional<UserData> findByUser_Id(Long userId);
}
