package com.novaserver.user.repository;

import com.novaserver.user.entity.UserData;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저 데이터 저장소. */
public interface UserDataRepository extends JpaRepository<UserData, Long> {
    Optional<UserData> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserData d where d.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from UserData d where d.user.id = :userId")
    Optional<UserData> findByUserIdForUpdate(@Param("userId") Long userId);
}
