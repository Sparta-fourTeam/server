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

    /**
     * MySQL 방언은 jakarta.persistence.lock.timeout을 0(NOWAIT)/-2(SKIP LOCKED)로만 해석한다. 그 외 값은 무시되고
     * innodb_lock_wait_timeout(기본 50초)까지 그냥 대기하게 되므로, 요청 스레드가 오래 묶이는 것을 막기 위해 0(NOWAIT)으로 즉시 실패하도록
     * 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select e from UserEnergy e where e.user.id = :userId")
    Optional<UserEnergy> findByUserIdForUpdate(@Param("userId") Long userId);

    @Modifying
    @Query("delete from UserEnergy e where e.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
