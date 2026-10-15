package com.novaserver.user.repository;

import com.novaserver.user.entity.UserEnergy;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

/** 유저 에너지 저장소. */
public interface UserEnergyRepository extends JpaRepository<UserEnergy, Long> {
    Optional<UserEnergy> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select e from UserEnergy e where e.user.id = :userId")
    Optional<UserEnergy> findByUserIdForUpdate(@Param("userId") Long userId);

    @Modifying
    @Query("delete from UserEnergy e where e.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
